package org.esa.s3tbx.c2rcc.ancillary;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.esa.snap.util.StringUtils;
import org.esa.snap.util.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AncDownloader {

    //    private final String downloadUrl = "http://oceandata.sci.gsfc.nasa.gov/cgi/getfile/";
    private final String downloadUrl;

    public AncDownloader(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public File download(File[] destFiles) throws IOException {
        String searchPattern = extractPrefix(destFiles[0]) + "*";
        final String[] downloadableFiles = getDownloadableFiles(searchPattern);

        for (File destFile : destFiles) {
            final String fileName = destFile.getName();
            if (StringUtils.contains(downloadableFiles, fileName)) {
                final String downloadUri = downloadUrl + fileName;
                final File downloadedFile = downloadFileTo(destFile, downloadUri);
                if (downloadedFile.isFile()) {
                    return downloadedFile;
                }
            }
        }
        return null;
    }

    private String extractPrefix(File destFile) {
        final String filename = destFile.getName();
        return filename.substring(0, filename.indexOf("_") + 1);
    }

    private static File downloadFileTo(File destFile, String downloadUri) throws IOException {
        final String USER_AGENT = "Chrome/44.0.2403.157";
        final HttpGet get = new HttpGet(downloadUri);
        get.setHeader("User-Agent", USER_AGENT);

        final DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(get);
        SystemUtils.LOG.info("\nSending 'GET' request to URL : " + downloadUri);
        SystemUtils.LOG.info("Response Code : " + response.getStatusLine().getStatusCode());

        final InputStream inputStream = response.getEntity().getContent();
        final String filename = destFile.getName();
        final File destPath = destFile.getParentFile();
        if (!destPath.exists()) {
            destPath.mkdirs();
        }
        if (!destPath.isDirectory()) {
            throw new IOException("The denoted path '" + destPath.getAbsolutePath() + "' is not a directory");
        }
        final String tempFileName = "tmp_" + filename;
        final File tempFile = new File(destPath, tempFileName);
        FileOutputStream streamOut = null;
        try {
            streamOut = new FileOutputStream(tempFile);
            final byte[] buffer = new byte[100000];
            int len;
            try {
                while ((len = inputStream.read(buffer)) > -1) {
                    streamOut.write(buffer, 0, len);
                }
            } finally {
                streamOut.close();
            }
            if (tempFile.renameTo(destFile)) {
                SystemUtils.LOG.info("The ancilarry file '" + destFile.getAbsolutePath() + "' has been writen.");
                return destFile;
            } else {
                SystemUtils.LOG.info("Unable to download the ancilarry file '" + destFile.getAbsolutePath() + "'.");
                return null;
            }
        } finally {
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

    private static String[] getDownloadableFiles(String searchPattern) throws IOException {
        final String uri = "http://oceandata.sci.gsfc.nasa.gov/search/file_search.cgi";
        List<NameValuePair> urlParameters = new ArrayList<>();
        //        urlParameters.add(new BasicNameValuePair("subID", ""));
        urlParameters.add(new BasicNameValuePair("std_only", "1"));
        urlParameters.add(new BasicNameValuePair("sensor", "all"));
        urlParameters.add(new BasicNameValuePair("search", searchPattern));
        //        urlParameters.add(new BasicNameValuePair("sdate", ""));
        urlParameters.add(new BasicNameValuePair("results_as_file", "1"));
        //        urlParameters.add(new BasicNameValuePair("edate", ""));
        urlParameters.add(new BasicNameValuePair("dtype", "anc"));
        urlParameters.add(new BasicNameValuePair("addurl", "1"));
        //        urlParameters.add(new BasicNameValuePair(".state", "Search"));
        //        urlParameters.add(new BasicNameValuePair(".cgifields", "results_as_file"));
        //        urlParameters.add(new BasicNameValuePair(".cgifields", "dtype"));
        //        urlParameters.add(new BasicNameValuePair(".cgifields", "addurl"));
        //        urlParameters.add(new BasicNameValuePair(".cgifields", "std_only"));
        //        urlParameters.add(new BasicNameValuePair(".cgifields", "sensor"));
        //        urlParameters.add(new BasicNameValuePair(".cgifields", "cksum"));

        final HttpPost post = new HttpPost(uri);
        post.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.157 Safari/537.36");
        //        post.setHeader("User-Agent", USER_AGENT);

        post.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        post.addHeader("Accept-Encoding", "gzip, deflate");
        post.setHeader("Accept-Language", "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");
        post.setHeader("Cache-Control", "max-age=0");
        post.setHeader("Connection", "keep-alive");
        //        post.setHeader("Content-Length", "229");
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setHeader("Cookie", "fsr.r=%7B%22d%22%3A90%2C%22i%22%3A%22d445cf0-83722180-13f0-4007-9efbe%22%2C%22e%22%3A1439906531011%7D; _ga=GA1.2.1089912019.1438162152");
        post.setHeader("Host", "oceandata.sci.gsfc.nasa.gov");
        post.setHeader("Origin", "http://oceandata.sci.gsfc.nasa.gov");
        post.setHeader("Referer", "http://oceandata.sci.gsfc.nasa.gov/search/file_search.cgi");
        post.setHeader("Upgrade-Insecure-Requests", "1");


        post.setEntity(new UrlEncodedFormEntity(urlParameters));

        final DefaultHttpClient client = new DefaultHttpClient();
        HttpResponse response = client.execute(post);
        //        System.out.println("\nSending 'POST' request to URL : " + uri);
        //        System.out.println("Post parameters : " + post.getEntity());
        //        System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

        final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));
        List<String> filenames = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            final String trimedLine = line.trim();
            final String lowLine = trimedLine.toLowerCase();
            if (lowLine.startsWith("http")) {
                final String filename = trimedLine.substring(trimedLine.lastIndexOf("/") + 1);
                filenames.add(filename);
            }
        }
        return filenames.toArray(new String[filenames.size()]);
    }
}
