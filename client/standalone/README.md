# Desktop/CLI client

### Supported device types
* (default) = any desktop platform supported by Java8 SE (Windows, Mac, Linux)
* model1 = Rasberry Pi 3 model B + Google Voice Kit

#### Building GraalVM native image
```
/opt/graalvm/bin/native-image -jar promethist.jar -H:+ReportExceptionStackTraces --no-fallback --allow-incomplete-classpath --no-server
```
(remove `--no-fallback --allow-incomplete-classpath` to get just bootstrap image)

#### Building OS package
see https://centerkey.com/mac/java/ for more information
```
javapackager -deploy -native pkg -name Promethist -BappVersion=2.0.0 -Bicon=Promethist.icns -srcdir target -srcfiles promethist.jar -appclass ai.promethist.standalone.Application -outdir target -outfile p -v
scp target/bundles/* jump.promethist.ai:/ext/cluster/default/default/repository/dist
```

#### Client version
```
java -jar promethist.jar version
```

#### Bot client
```
# no auto update
java -jar promethist.jar bot -u https://port.preview.promethist.ai -k brainquist -s RPT01 -l cs -na

# no input/output audio (just texting)
java -jar promethist.jar bot -u https://port.preview.promethist.ai -k brainquist -s RPT01 -l cs -nia -noa

# automated run from/to text file
java -jar promethist.jar bot -u https://port.preview.promethist.ai -k performance-test -s RPT01 -l en -na -nia -noa -nol -ex -i input.txt -o output.txt
```

#### Tool actions
Processing TTS to MP3 file using platform TTS request default values - see https://gitlab.promethist.ai/services/port/blob/develop/api/src/main/kotlin/com/promethistai/port/tts/TtsRequest.kt
```
java -jar promethist.jar tool -a tts -u http://localhost:8080 -k test1 -i "tohle je test a ďábelský kůň který pěl ódy" -o local/test.mp3 -l cs
```

Converting MP3 to PCM file
```
java -jar promethist.jar tool -a play -i local/test.mp3 -o local/test.pcm
```

Mixing MP3 or PCM with microphone input to PCM file
```
java -jar promethist.jar tool -a play -i local/test.pcm -m -o local/test2.pcm
```

Convert PCM to WAV
```
ffmpeg -f s16le -ar 16k -ac 1 -i test.pcm test.wav
```