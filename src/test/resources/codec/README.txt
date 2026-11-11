PNS regression fixture: pns-synthetic.mp4
Synthetic pink noise generated specifically for this test, not a third-party song.

Reference generation (FFmpeg 6.1.1):
ffmpeg -f lavfi -i anoisesrc=color=pink:sample_rate=44100:duration=2:seed=427
  -ac 2 -c:a aac -b:a 96k -aac_pns 1
  -movflags +frag_keyframe+empty_moov+default_base_moof pns-synthetic.mp4

88 decoded AAC frames, 44.1 kHz stereo PCM16. The preceding decoder emitted
98.865% zero samples and 176,768 non-finite float samples. The corrected decoder
must preserve sample count, finite energy, and deterministic independent state.
The reference executable is not part of the mod or required for these tests.
