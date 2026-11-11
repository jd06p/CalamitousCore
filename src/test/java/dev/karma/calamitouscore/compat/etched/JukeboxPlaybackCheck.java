package dev.karma.calamitouscore.compat.etched;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.openal.*;
import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.CRC32;

/** Actual Minecraft Channel + OpenAL Soft loopback: no Minecraft world, speakers or wall-clock sleeps. */
public final class JukeboxPlaybackCheck {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static Channel channel()throws Exception {
        var create=Channel.class.getDeclaredMethod("create");create.setAccessible(true);return (Channel)create.invoke(null);
    }
    private static byte[] wave(int seconds) {
        int size=44100*4*seconds;ByteBuffer bytes=ByteBuffer.allocate(44+size).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(0x46464952).putInt(36+size).putInt(0x45564157).putInt(0x20746d66).putInt(16).putShort((short)1).putShort((short)2);
        bytes.putInt(44100).putInt(176400).putShort((short)4).putShort((short)16).putInt(0x61746164).putInt(size);
        for(int frame=0;frame<44100*seconds;frame++){bytes.putShort((short)(frame%1000));bytes.putShort((short)(-frame%1000));}
        return bytes.array();
    }
    public static void main(String[] args)throws Exception {
        var tag=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/sophisticatedbackpacks/tags/items/upgrade.json"))).getAsJsonObject();
        check(!tag.get("replace").getAsBoolean()&&tag.getAsJsonArray("values").size()==2,"Tag must append only the two jukebox items");
        check("sophisticatedbackpacks:advanced_jukebox_upgrade".equals(JukeboxUpgradeCompat.counterpart("backpack","sophisticatedstorage:advanced_jukebox_upgrade")),"Advanced alias");
        check(JukeboxUpgradeCompat.counterpart("chest","sophisticatedstorage:advanced_jukebox_upgrade")==null,"Storage behavior changed");
        check(JukeboxUpgradeCompat.counterpart("backpack","sophisticatedstorage:crafting_upgrade")==null,"Unrelated upgrade changed");
        System.out.println("PASS: narrow compatibility tag and backpack-only limit delegation policy.");
        byte[] wave=wave(10);
        try(var prepared=PreparedSoundCloudStream.prepare(new ByteArrayInputStream(wave),true,false)) {
            ByteBuffer bytes=prepared.completeData();check(bytes.remaining()==wave.length-44,"Stereo length changed");
            for(int frame=0;frame<441000;frame++){check(bytes.getShort()==(short)(frame%1000),"Left sample changed");check(bytes.getShort()==(short)(-frame%1000),"Right sample changed");}
        }
        try(var mono=PreparedSoundCloudStream.prepare(new ByteArrayInputStream(wave),false,false)) {
            ByteBuffer bytes=mono.completeData();check(bytes.remaining()==441000*2,"Mono duration changed");
            for(int frame=0;frame<441000;frame++)check(bytes.getShort()==(short)(frame%1000),"Existing Etched mono selection changed");
        }
        System.out.println("PASS: complete stereo samples/rate/duration preserved; existing positional mono semantics preserved.");
        byte[] mp3=new byte[417*100];
        for(int frame=0;frame<100;frame++){int at=frame*417;mp3[at]=(byte)0xff;mp3[at+1]=(byte)0xfb;mp3[at+2]=(byte)0x90;}
        try(var legacy=PreparedSoundCloudStream.prepareMp3(new ByteArrayInputStream(mp3),true,false)) {
            check(legacy.getFormat().getSampleRate()==44100 && legacy.completeData().remaining()==100*1152*4,"Legacy MP3 samples or duration changed");
        }
        System.out.println("PASS: legacy MP3 uses Etched's decoder and prepares the complete decoded sample count.");
        if(args.length>0)try(var paths=Files.list(Path.of(args[0]))) {
            int verified=0;
            for(Path file:paths.filter(path->path.toString().endsWith(".audio")).toList()) {
                SoundCloudAudio.validate(file,0);
                try(var input=Files.newInputStream(file);var prepared=PreparedSoundCloudStream.prepare(input,true,false)) {
                    CRC32 actual=new CRC32();actual.update(prepared.completeData());CRC32 expected=new CRC32();
                    try(var source=Files.newInputStream(file)){source.skipNBytes(44);byte[] block=new byte[65536];for(int count;(count=source.read(block))!=-1;)expected.update(block,0,count);}
                    check(actual.getValue()==expected.getValue(),"Cached stereo PCM samples changed: "+file.getFileName());verified++;
                }
            }
            check(verified>0,"No cached tracks tested");System.out.println("PASS: "+verified+" real cached SoundCloud tracks completely prepared with identical stereo PCM checksums.");
        }
        long device=SOFTLoopback.alcLoopbackOpenDeviceSOFT((ByteBuffer)null);
        check(device!=0,"OpenAL Soft loopback unavailable");
        long context=ALC10.alcCreateContext(device,new int[]{SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT,SOFTLoopback.ALC_STEREO_SOFT,SOFTLoopback.ALC_FORMAT_TYPE_SOFT,SOFTLoopback.ALC_SHORT_SOFT,ALC10.ALC_FREQUENCY,44100,0});
        check(context!=0,"Loopback context unavailable");ALC10.alcMakeContextCurrent(context);AL.createCapabilities(ALC.createCapabilities(device));
        Channel streamed=null,preparedChannel=null;SoundBuffer owned=null;
        try {
            streamed=channel();preparedChannel=channel();
            streamed.attachBufferStream(new AudioStream(){
                int remaining=44100*4*10;
                public AudioFormat getFormat(){return new AudioFormat(44100,16,2,true,false);}
                public ByteBuffer read(int count){int length=Math.min(count,remaining);remaining-=length;return ByteBuffer.allocateDirect(length);}
                public void close(){}
            });
            streamed.play();
            var prepared=PreparedSoundCloudStream.prepare(new ByteArrayInputStream(wave),true,false);
            owned=prepared.attachTo(preparedChannel);preparedChannel.play();
            SOFTLoopback.alcRenderSamplesSOFT(device,new short[44100*6*2],44100*6);
            check(streamed.stopped(),"Expected vanilla four-second queue underrun");
            check(preparedChannel.playing(),"Prepared full track stopped before completion");
            streamed.updateStream();check(streamed.stopped(),"Expected native stopped stream not to restart itself");
            preparedChannel.pause();SOFTLoopback.alcRenderSamplesSOFT(device,new short[44100*2],44100);
            check(!preparedChannel.stopped(),"Pause became completion");preparedChannel.unpause();
            SOFTLoopback.alcRenderSamplesSOFT(device,new short[44100*5*2],44100*5);
            check(preparedChannel.stopped(),"Prepared track did not end naturally");
            check(AL10.alGetError()==AL10.AL_NO_ERROR,"OpenAL error");
            System.out.println("PASS: native streamed queue underruns after six seconds without refills; prepared audio keeps playing and ends naturally after its full duration.");
            preparedChannel.destroy();owned.discardAlBuffer();owned=null;preparedChannel=channel();
            owned=PreparedSoundCloudStream.prepare(new ByteArrayInputStream(wave(1)),true,true).attachTo(preparedChannel);preparedChannel.play();
            SOFTLoopback.alcRenderSamplesSOFT(device,new short[44100*6*2],44100*6);
            check(preparedChannel.playing(),"Looping audio ended early");preparedChannel.stop();check(preparedChannel.stopped(),"Manual stop failed");
            System.out.println("PASS: native looping and manual stop retain their expected behavior.");
        } finally {
            if(streamed!=null)streamed.destroy();if(preparedChannel!=null)preparedChannel.destroy();if(owned!=null)owned.discardAlBuffer();
            ALC10.alcMakeContextCurrent(0);ALC10.alcDestroyContext(context);ALC10.alcCloseDevice(device);
        }
        System.out.println("PASS: native playback resources released; no speakers or running game used.");
    }
}
