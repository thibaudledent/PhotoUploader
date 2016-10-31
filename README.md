# PhotoUploader
Resize and upload images via FTP

## Usage

1. Create a file `config.properties` with your credentials and access to your FTP: 
```
address=ftp.example.be
user=myuser
password=mypassword
```

2. Put `config.properties`, `wget.exe`, `uploadPhotos.bat`, `PhotoUploader.jar`<sup>[1](#footnote1)</sup> in the same folder alongside the photos you want to upload

3. Run `uploadPhotos.bat`

<a name="footnote1">1</a>: `PhotoUploader.jar` can be found in `out\artifacts\PhotoUploader_jar`


## How to build jars from IntelliJ properly?
- `File -> Project Structure -> Project Settings -> Artifacts -> Jar -> From modules with dependencies...`
- Extract to the target Jar
- OK
- Build | Build Artifact

NB: Make sure your `MANIFEST.MF` is in:
 - `src/main/resources/META_INF/`
 - and NOT in `src/main/java/META_INF/`
