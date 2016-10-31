import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public class MainClass {
    /**
     * Array of supported extensions.
     */
    static final String[] EXTENSIONS = new String[]{
            "jpg", "png", "bmp", "jpeg"
    };

    /**
     * Filter to identify images based on their extensions.
     */
    static final FilenameFilter IMAGE_FILTER = (dir1, name) -> {
        for (final String ext : EXTENSIONS) {
            if (name.endsWith("." + ext)) {
                return (true);
            }
        }
        return (false);
    };

    /**
     * Size of the image after it has been resized.
     */
    static final int IMAGE_SIZE_AFTER_RESIZE = 600;

    /**
     * Path to the ftp gallery directory.
     */
    static final String FTP_DIRECTORY = "/public_html";
    static final String FTP_GALLERY = "/img/gallery/";
    static final String FTP_GALLERY_DIRECTORY = FTP_DIRECTORY + FTP_GALLERY;

    /**
     * The entry point of application.
     * @param args the input arguments
     */
    public static void main(String[] args) {
        final String currentDirectoryStr = System.getProperty("user.dir");
        final File currentDirectory = new File(currentDirectoryStr);
        System.out.println("Working Directory = " + currentDirectoryStr);

        // Get properties
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(currentDirectoryStr + "\\config.properties"));
        } catch (IOException e) {
            System.out.println("Missing file config.properties");
            return;
        }

        final String ftpAddress = properties.getProperty("address");
        final String user = properties.getProperty("user");
        final String password = properties.getProperty("password");

        if (!StringUtils.isNoneBlank(ftpAddress, user, password)) {
            System.out.println("Cannot get properties");
            return;
        }

        // Create output directory to store resized images
        final File outputDirectory = new File(currentDirectoryStr + "/" + "imagesToUpload");
        if (!createDirectory(outputDirectory)) {
            System.out.println("Cannot create directory");
            return;
        }

        // Process images
        processImages(outputDirectory, currentDirectory, ftpAddress, user, password);

        System.out.println("Done!");
    }

    private static void processImages(File destinationDir, File currentDirectory, String ftpAddress, String user, String password) {
        File[] listOfFiles = currentDirectory.listFiles(IMAGE_FILTER);

        if (listOfFiles == null) {
            System.out.println("Cannot fetch list of image files");
            return;
        }

        for (File image : listOfFiles) {
            if (image.isDirectory()) {
                return;
            }
            System.out.println("Processing image: " + image.getName() + "...");

            resize(destinationDir, image);
            upload(destinationDir, image.getName(), ftpAddress, user, password);
        }

        updateGallery(currentDirectory, ftpAddress, user, password);
    }

    private static void updateGallery(File currentDirectory, String ftpAddress, String user, String password) {
        FTPClient client = new FTPClient();
        StringBuilder htmlContentBuilder = new StringBuilder();

        try {
            client.connect(ftpAddress);
            client.login(user, password);

            // Store file to server
            FTPFile[] files = client.listFiles(FTP_GALLERY_DIRECTORY);

            for (FTPFile file : files) {
                if (!isImage(file)) {
                    continue;
                }

                appendHtml(htmlContentBuilder, file);
            }

            final String htmlFileName = "gallery_content.html";
            final String pathToFile = currentDirectory.getPath() + "/" + htmlFileName;
            if (!createHtmlFile(htmlContentBuilder, pathToFile)) {
                System.out.println("Html file not created, cannot upload.");
            }

            FileInputStream fis = new FileInputStream(pathToFile);
            final String ftpPathToFile = FTP_DIRECTORY + "/" + htmlFileName;
            System.out.println("Uploading file to " + ftpPathToFile);
            if (client.storeFile(ftpPathToFile, fis)) {
                System.out.println("Uploading SUCCESS");
            } else {
                System.out.println("Uploading FAILURE");
            }
            client.logout();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void appendHtml(StringBuilder htmlContentBuilder, FTPFile file) {
        htmlContentBuilder.append("\n\n<div class=\"col-lg-3 col-md-4 col-xs-6 thumb\">\n" +
                "\t<a class=\"thumbnail\" href=\"" +
                FTP_GALLERY + file.getName() +
                "\">\n" +
                "\t\t<img class=\"img-responsive\" src=\"" +
                FTP_GALLERY + file.getName() +
                "\" alt=\"\">\n" +
                "\t</a>\n" +
                "</div>");
    }

    private static boolean createHtmlFile(StringBuilder htmlContentBuilder, String pathname) {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(new File(pathname)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write contents of the string builder to a file
        if (bufferedWriter == null) {
            System.out.println("Cannot create buffer writer for file gallery_content.html");
            return false;
        }

        final String htmlContent = htmlContentBuilder.toString();
        if (StringUtils.isBlank(htmlContent)) {
            System.out.println("No content to write into file gallery_content.html");
            return false;
        }

        try {
            bufferedWriter.write(htmlContent);
            bufferedWriter.flush();
            bufferedWriter.close();
            System.out.println("Content written to file gallery_content.html.");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isImage(FTPFile file) {
        boolean isImage = false;
        for (final String ext : EXTENSIONS) {
            if (file.getName().endsWith("." + ext)) {
                isImage = true;
            }
        }
        return isImage;
    }

    private static void upload(File directory, String imageName, String ftpAddress, String user, String password) {
        final String pathToImageResized = directory.getPath() + "\\" + imageName;
        System.out.println("Path to image resized: " + pathToImageResized);
        File imageResized = new File(pathToImageResized);

        if (!imageResized.isFile()) {
            System.out.println("Cannot get resized image");
        } else {
            System.out.println("Uploading " + pathToImageResized + "...");
        }

        FTPClient client = new FTPClient();

        try (FileInputStream fis = new FileInputStream(imageResized)) {
            client.connect(ftpAddress);
            client.login(user, password);

            // Binary mode needed to transfer images (which aren't text files and cannot be encoded in ASCII )
            client.setFileType(FTP.BINARY_FILE_TYPE, FTP.BINARY_FILE_TYPE);
            client.setFileTransferMode(FTP.BINARY_FILE_TYPE);

            // Store file to server
            if (client.storeFile(FTP_GALLERY_DIRECTORY + imageResized.getName(), fis)) {
                System.out.println("Uploading SUCCESS");

            } else {
                System.out.println("Uploading FAILURE");
            }
            client.logout();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void resize(File destinationDir, File image) {
        try {
            Thumbnails.of(image)
                    .size(IMAGE_SIZE_AFTER_RESIZE, IMAGE_SIZE_AFTER_RESIZE)
                    .toFiles(destinationDir, Rename.NO_CHANGE);
            System.out.println("Image resized");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean createDirectory(File directory) {
        // If the directory does not exist, create it
        if (!directory.exists()) {
            System.out.println("Creating directory: " + directory.getName());

            try {
                return directory.mkdir();
            } catch (SecurityException se) {
                se.printStackTrace();
                return false;
            }
        }
        System.out.println("Directory already exists: " + directory.getName());
        return true;
    }
}