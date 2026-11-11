package dev.karma.calamitouscore.compat.etched;

import dev.karma.calamitouscore.shaded.jaad.aac.*;
import dev.karma.calamitouscore.shaded.jaad.aac.syntax.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Synthetic PNS regression: old decoder produced ~99% zeros despite correct duration and file length. */
public final class SoundCloudPnsCheck {
    interface Checked {void run() throws Exception;}
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void fails(Checked action)throws Exception {try{action.run();}catch(IOException expected){return;}throw new AssertionError("Invalid AAC was accepted");}
    static byte[] decode(Path fixture)throws Exception {
        Path wave=SoundCloudAac.decode(fixture,2000);
        try {
            SoundCloudAudio.validate(wave,2000);
            byte[] bytes=Files.readAllBytes(wave);
            var pcm=ByteBuffer.wrap(bytes,44,bytes.length-44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            int zeros=0,count=pcm.remaining();double energy=0;
            while(pcm.hasRemaining()){int value=pcm.get();if(value==0)zeros++;energy+=(double)value*value;}
            check(count==88*1024*2,"Decoded sample count changed");
            check(zeros/(double)count<.05,"PNS silence/cut regression");
            check(Math.sqrt(energy/count)>3000 && Math.sqrt(energy/count)<6000,"PNS energy out of expected range");
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        }finally{Files.deleteIfExists(wave);}
    }
    public static void main(String[] args)throws Exception {
        Path fixture=Files.createTempFile("pns-regression-", ".mp4");
        try(InputStream resource=SoundCloudPnsCheck.class.getResourceAsStream("/codec/pns-synthetic.mp4")) {
            if(resource==null)throw new AssertionError("Missing synthetic fixture");Files.copy(resource,fixture,StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            byte[] first=decode(fixture);check(Arrays.equals(first,decode(fixture)),"Sequential decoder state leaked");
            ExecutorService workers=Executors.newFixedThreadPool(4);
            try {
                List<Future<byte[]>> futures=new ArrayList<>();for(int i=0;i<8;i++)futures.add(workers.submit(()->decode(fixture)));
                for(var future:futures)check(Arrays.equals(first,future.get()),"Concurrent decoder state leaked");
            }finally{workers.shutdownNow();}
            check(!java.lang.reflect.Modifier.isStatic(ICStream.class.getDeclaredField("randomState").getModifiers()),"Global shared noise state returned");
            System.out.println("PASS: PNS sample count/energy, no silence collapse, repeated and eight concurrent decodes are deterministic.");
            Decoder decoder=new Decoder(new byte[]{0x12,0x10});
            var syn=new SyntacticElements(decoder.getConfig());var data=SyntacticElements.class.getDeclaredField("data");data.setAccessible(true);
            for(float invalid:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}) {
                float[][] channels=new float[2][1024];channels[0][5]=invalid;data.set(syn,channels);fails(()->syn.sendToOutput(new SampleBuffer()));
            }
            fails(()->new Decoder(new byte[]{0x12,0x10}).decodeFrame(new byte[]{0x21},new SampleBuffer()));
            System.out.println("PASS: NaN, infinities and truncated AAC frames are rejected rather than quantized to a successful silent WAV.");
            Path previous=Path.of("example-game/etched-sounds/calamitouscore-soundcloud");
            Path current=SoundCloudClientAudio.cacheFolder(Path.of("example-game"));
            check(current.startsWith(previous) && !current.equals(previous),"Damaged previous-generation WAV cache would be reused");
            check(current.equals(SoundCloudClientAudio.cacheFolder(Path.of("example-game"))),"Cache namespace not restart-stable");
            System.out.println("PASS: previous damaged cache excluded; corrected cache namespace is deterministic across restarts.");
        }finally{Files.deleteIfExists(fixture);}
    }
}
