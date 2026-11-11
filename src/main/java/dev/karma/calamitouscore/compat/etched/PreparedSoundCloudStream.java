package dev.karma.calamitouscore.compat.etched;

import gg.moonflower.etched.api.util.WaveDataReader;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.lang.ref.Cleaner;
import java.nio.*;
import java.nio.file.*;
import gg.moonflower.etched.api.util.Mp3InputStream;

/** Complete PCM prepared off the sound thread, handed to OpenAL once, then explicitly released. */
public final class PreparedSoundCloudStream implements AudioStream {
    private static final Cleaner CLEANER=Cleaner.create();
    private final AudioFormat format;
    private final boolean looping;
    private ByteBuffer data;
    private final Cleaner.Cleanable cleanup;
    private PreparedSoundCloudStream(AudioFormat format,ByteBuffer data,boolean looping) {
        this.format=format;this.data=data;this.looping=looping;
        long address=MemoryUtil.memAddress(data);
        cleanup=CLEANER.register(this,()->MemoryUtil.nmemFree(address));
    }
    public static PreparedSoundCloudStream prepare(InputStream source,boolean stereo,boolean looping) throws IOException {
        try(var input=WaveDataReader.getAudioInputStream(new BufferedInputStream(source))) {
            AudioFormat original=input.getFormat();
            int sourceChannels=original.getChannels(),channels=stereo?sourceChannels:1;
            long frames=input.getFrameLength(),size=frames*channels*2;
            if(original.getSampleSizeInBits()!=16 || sourceChannels<1 || sourceChannels>2 || frames<=0 || size>512L*1024*1024)
                throw new IOException("Unsupported prepared SoundCloud PCM format");
            ByteBuffer output=MemoryUtil.memAlloc((int)size).order(ByteOrder.nativeOrder());boolean success=false;
            try {
                byte[] chunk=new byte[65536]; long bytes=0;
                for(int count;(count=input.read(chunk))!=-1;) {
                    if(count==0) throw new IOException("SoundCloud PCM made no read progress");
                    if(count%(sourceChannels*2)!=0) throw new IOException("Unaligned SoundCloud PCM frame");
                    bytes+=count;
                    if(bytes>frames*sourceChannels*2) throw new IOException("SoundCloud PCM exceeds declared length");
                    if(channels==sourceChannels && original.isBigEndian()==(ByteOrder.nativeOrder()==ByteOrder.BIG_ENDIAN)) output.put(chunk,0,count);
                    else for(int at=0;at<count;at+=sourceChannels*2) for(int channel=0;channel<channels;channel++) {
                        int offset=at+channel*2;
                        short sample=(short)(original.isBigEndian()?((chunk[offset]&255)<<8)|(chunk[offset+1]&255):((chunk[offset+1]&255)<<8)|(chunk[offset]&255));
                        output.putShort(sample);
                    }
                }
                if(bytes!=frames*sourceChannels*2) throw new IOException("Truncated prepared SoundCloud PCM");
                output.flip();
                AudioFormat format=new AudioFormat(original.getSampleRate(),16,channels,true,ByteOrder.nativeOrder()==ByteOrder.BIG_ENDIAN);
                var prepared=new PreparedSoundCloudStream(format,output,looping);success=true;return prepared;
            } finally {if(!success)MemoryUtil.memFree(output);}
        } catch(javax.sound.sampled.UnsupportedAudioFileException e) {throw new IOException("Not a prepared SoundCloud WAV",e);}
    }
    /** Legacy MP3s use Etched's existing decoder, fully decoded before the OpenAL channel starts. */
    public static PreparedSoundCloudStream prepareMp3(InputStream source,boolean stereo,boolean looping) throws IOException {
        Path wave=Files.createTempFile("calamitouscore-prepared-mp3-", ".wav");
        try {
            try(var mp3=new Mp3InputStream(new BufferedInputStream(source));var output=new RandomAccessFile(wave.toFile(),"rw")) {
                AudioFormat format=mp3.getFormat();int channels=format.getChannels(),rate=(int)format.getSampleRate();
                if(format.getSampleSizeInBits()!=16 || channels<1 || channels>2 || rate<=0)throw new IOException("Unsupported decoded SoundCloud MP3 format");
                output.write(new byte[44]);byte[] block=new byte[65536];long total=0;
                for(int count;(count=mp3.read(block))!=-1;) {
                    if(count==0 || (count&1)!=0)throw new IOException("Invalid decoded MP3 sample block");
                    total+=count;if(total>512L*1024*1024)throw new IOException("Decoded SoundCloud MP3 exceeds 512 MiB");
                    if(format.isBigEndian())for(int at=0;at<count;at+=2){byte first=block[at];block[at]=block[at+1];block[at+1]=first;}
                    output.write(block,0,count);
                }
                if(total==0 || total%(channels*2)!=0)throw new IOException("Empty or unaligned decoded SoundCloud MP3");
                ByteBuffer header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
                header.putInt(0x46464952).putInt((int)total+36).putInt(0x45564157).putInt(0x20746d66).putInt(16).putShort((short)1).putShort((short)channels);
                header.putInt(rate).putInt(rate*channels*2).putShort((short)(channels*2)).putShort((short)16).putInt(0x61746164).putInt((int)total);
                output.seek(0);output.write(header.array());
            }
            return prepare(Files.newInputStream(wave),stereo,looping);
        } finally {Files.deleteIfExists(wave);}
    }
    public boolean looping(){return looping;}
    public SoundBuffer attachTo(Channel channel) throws IOException {
        SoundBuffer buffer=new SoundBuffer(completeData(),format);
        try {channel.attachStaticBuffer(buffer);channel.setLooping(looping);return buffer;}
        catch(RuntimeException failure){buffer.discardAlBuffer();throw failure;}
        finally{close();}
    }
    public ByteBuffer completeData() throws IOException {
        if(data==null) throw new IOException("Prepared SoundCloud stream is closed");
        return data.duplicate().order(ByteOrder.nativeOrder());
    }
    @Override public AudioFormat getFormat(){return format;}
    @Override public ByteBuffer read(int amount) throws IOException {
        if(amount<0)throw new IOException("Negative audio read");
        ByteBuffer source=completeData();int length=Math.min(amount,source.remaining());
        ByteBuffer slice=source.slice().order(ByteOrder.nativeOrder());slice.limit(length);data.position(data.position()+length);return slice;
    }
    @Override public void close(){data=null;cleanup.clean();}
}
