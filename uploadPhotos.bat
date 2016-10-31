@echo off 
echo Check for updates... 
wget -O PhotoUploader.jar https://github.com/thibaudledent/PhotoUploader/raw/master/out/artifacts/PhotoUploader_jar/PhotoUploader.jar >nul 2>&1
echo Ready to start the upload? All your photos must be in the current directory
PAUSE
start java -jar PhotoUploader.jar
PAUSE