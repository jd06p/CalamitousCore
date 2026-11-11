package dev.karma.calamitouscore.compat.etched;

import dev.karma.calamitouscore.shaded.jaad.aac.Decoder;
import dev.karma.calamitouscore.shaded.jaad.aac.SampleBuffer;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Strict single-audio-track ISO-BMFF demux for SoundCloud's unencrypted fragmented AAC-LC HLS. */
public final class SoundCloudAac {
    private static final long MAX_PCM = 512L * 1024 * 1024;
    private record Box(String type, int start, int data, int end) {}
    private SoundCloudAac() {}

    private static int integer(byte[] bytes, int at) throws IOException {
        if (at < 0 || at + 4 > bytes.length) throw new IOException("Truncated AAC container field");
        return ByteBuffer.wrap(bytes, at, 4).getInt();
    }
    private static List<Box> boxes(byte[] bytes, int start, int end) throws IOException {
        List<Box> result = new ArrayList<>();
        for (int at = start; at < end;) {
            if (end - at < 8) throw new IOException("Truncated AAC box header");
            long size = Integer.toUnsignedLong(integer(bytes, at)); int header = 8;
            if (size == 1) { if (end-at < 16) throw new IOException("Truncated AAC extended box"); size = ByteBuffer.wrap(bytes, at+8,8).getLong(); header=16; }
            else if (size == 0) size = end-at;
            if (size < header || size > end-at) throw new IOException("Invalid AAC box length");
            result.add(new Box(new String(bytes,at+4,4,StandardCharsets.US_ASCII),at,at+header,at+(int)size));
            at += (int)size;
        }
        return result;
    }
    private static Box find(byte[] data, Box parent, String type) throws IOException {
        return boxes(data,parent.data,parent.end).stream().filter(box -> box.type.equals(type)).findFirst().orElseThrow(() -> new IOException("AAC container missing " + type));
    }
    private static byte[] config(byte[] bytes, int start, int end) throws IOException {
        for (int at=start;at<end;) {
            int tag=bytes[at++]&255, length=0, count=0, part;
            do { if(at>=end || ++count>4) throw new IOException("Invalid AAC descriptor"); part=bytes[at++]&255; length=(length<<7)|(part&127); } while((part&128)!=0);
            if(length>end-at) throw new IOException("Truncated AAC descriptor");
            if(tag==5) return Arrays.copyOfRange(bytes,at,at+length);
            if(tag==3) {
                if(length<3) throw new IOException("Invalid AAC ES descriptor");
                int flags=bytes[at+2]&255, child=at+3;
                if((flags&128)!=0) child+=2;
                if((flags&64)!=0) { if(child>=at+length) throw new IOException("Invalid AAC URL descriptor"); child+=1+(bytes[child]&255); }
                if((flags&32)!=0) child+=2;
                return config(bytes,child,at+length);
            }
            if(tag==4) return config(bytes,at+13,at+length);
            at+=length;
        }
        throw new IOException("AAC AudioSpecificConfig not found");
    }

    public static Path decode(Path input, long expectedMillis) throws IOException {
        byte[] bytes = Files.readAllBytes(input);
        List<Box> top=boxes(bytes,0,bytes.length);
        Box moov=top.stream().filter(box->box.type.equals("moov")).findFirst().orElseThrow(()->new IOException("AAC initialization segment is missing"));
        Box trak=find(bytes,moov,"trak"), tkhd=find(bytes,trak,"tkhd");
        int trackId=integer(bytes,tkhd.data+((bytes[tkhd.data]&255)==1?20:12));
        Box mdia=find(bytes,trak,"mdia"), stsd=find(bytes,find(bytes,find(bytes,mdia,"minf"),"stbl"),"stsd");
        List<Box> descriptions=boxes(bytes,stsd.data+8,stsd.end);
        if(descriptions.size()!=1 || !descriptions.get(0).type.equals("mp4a")) throw new IOException("Only unencrypted AAC audio is supported");
        Box mp4a=descriptions.get(0);
        Box esds=boxes(bytes,mp4a.data+28,mp4a.end).stream().filter(box->box.type.equals("esds")).findFirst().orElseThrow(()->new IOException("AAC decoder configuration missing"));
        Decoder decoder;
        try { decoder=new Decoder(config(bytes,esds.data+4,esds.end)); }
        catch(Exception e) { throw new IOException("Unsupported AAC configuration",e); }
        int defaultSize=0;
        for(Box box:boxes(bytes,find(bytes,moov,"mvex").data,find(bytes,moov,"mvex").end))
            if(box.type.equals("trex") && integer(bytes,box.data+4)==trackId) defaultSize=integer(bytes,box.data+16);
        Path output=Files.createTempFile("calamitouscore-aac-", ".wav"); boolean success=false;
        long pcm=0; int rate=0, channels=0, bits=0, frames=0;
        SampleBuffer buffer=new SampleBuffer(); buffer.setBigEndian(false);
        try (RandomAccessFile wav=new RandomAccessFile(output.toFile(),"rw")) {
            wav.write(new byte[44]);
            for(Box moof:top) if(moof.type.equals("moof")) {
                for(Box traf:boxes(bytes,moof.data,moof.end)) if(traf.type.equals("traf")) {
                    Box tfhd=find(bytes,traf,"tfhd"); int flags=integer(bytes,tfhd.data)&0xffffff;
                    if(integer(bytes,tfhd.data+4)!=trackId) throw new IOException("Unexpected additional AAC track");
                    int at=tfhd.data+8; long base=moof.start;
                    if((flags&1)!=0) { base=ByteBuffer.wrap(bytes,at,8).getLong(); at+=8; }
                    if((flags&2)!=0) at+=4;
                    if((flags&8)!=0) at+=4;
                    int size=defaultSize;
                    if((flags&16)!=0) { size=integer(bytes,at); at+=4; }
                    long cursor=-1;
                    for(Box run:boxes(bytes,traf.data,traf.end)) if(run.type.equals("trun")) {
                        int runFlags=integer(bytes,run.data)&0xffffff, count=integer(bytes,run.data+4), pos=run.data+8;
                        if(count<0 || count>1000000) throw new IOException("Invalid AAC sample count");
                        if((runFlags&1)!=0) { cursor=base+integer(bytes,pos); pos+=4; }
                        if((runFlags&4)!=0) pos+=4;
                        if(cursor<0) cursor=top.stream().filter(box->box.type.equals("mdat")&&box.start>=moof.end).findFirst().orElseThrow(()->new IOException("AAC media data missing")).data;
                        for(int sample=0;sample<count;sample++) {
                            if((runFlags&0x100)!=0) pos+=4;
                            int sampleSize=size;
                            if((runFlags&0x200)!=0) { sampleSize=integer(bytes,pos); pos+=4; }
                            if((runFlags&0x400)!=0) pos+=4;
                            if((runFlags&0x800)!=0) pos+=4;
                            if(pos>run.end || sampleSize<=0 || cursor<0 || cursor+sampleSize>bytes.length) throw new IOException("Truncated AAC sample");
                            final long begin=cursor, end=cursor+sampleSize;
                            if(top.stream().noneMatch(box->box.type.equals("mdat")&&begin>=box.data&&end<=box.end)) throw new IOException("AAC sample lies outside media data");
                            try { decoder.decodeFrame(Arrays.copyOfRange(bytes,(int)cursor,(int)end),buffer); }
                            catch(Exception e) { throw new IOException("AAC frame failed validation",e); }
                            if(frames==0) { rate=buffer.getSampleRate();channels=buffer.getChannels();bits=buffer.getBitsPerSample(); }
                            if(rate<=0 || channels<1 || channels>2 || bits!=16 || rate!=buffer.getSampleRate() || channels!=buffer.getChannels()) throw new IOException("Unsupported changing AAC output format");
                            pcm+=buffer.getData().length;
                            if(pcm>MAX_PCM) throw new IOException("Decoded SoundCloud audio exceeds 512 MiB");
                            wav.write(buffer.getData()); frames++; cursor=end;
                        }
                    }
                }
            }
            if(frames==0 || pcm==0) throw new IOException("No AAC audio samples");
            SoundCloudAudio.checkDuration(pcm*1000.0/(rate*channels*2),expectedMillis);
            ByteBuffer header=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
            header.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt((int)pcm+36).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));
            header.putInt(16).putShort((short)1).putShort((short)channels).putInt(rate).putInt(rate*channels*2).putShort((short)(channels*2)).putShort((short)16);
            header.put("data".getBytes(StandardCharsets.US_ASCII)).putInt((int)pcm); wav.seek(0); wav.write(header.array()); success=true;
        } finally { if(!success) Files.deleteIfExists(output); }
        return output;
    }
}
