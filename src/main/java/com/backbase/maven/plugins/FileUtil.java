package com.backbase.maven.plugins;

import org.apache.maven.plugin.MojoFailureException;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

import static java.nio.file.FileVisitResult.*;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class FileUtil {

    private static class Finder
            extends SimpleFileVisitor< Path > {

        private PathMatcher matcher;
        ArrayList< Path > files = new ArrayList<>();

        Finder( String pattern ) {
            matcher = FileSystems.getDefault()
                    .getPathMatcher( "glob:" + pattern );
        }

        // Compares the glob pattern against
        // the file or directory name.
        void find( Path file ) {
            Path name = file.getFileName();
            if ( name != null && matcher.matches( name ) ) {
                files.add( file );
            }
        }

        // Prints the total number of
        // matches to standard out.
        public List< Path > search() {
            return files;
        }

        // Invoke the pattern matching
        // method on each file.
        @Override
        public FileVisitResult visitFile( Path file,
                                          BasicFileAttributes attrs ) {
            find( file );
            return CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory( Path dir,
                                                  BasicFileAttributes attrs ) {
            find( dir );
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed( Path file,
                                                IOException exc ) {
            System.err.println( exc );
            return CONTINUE;
        }

    }

    public static List< Path > Search( String pattern, String path, int maxDepth )
            throws IOException {

        Finder finder = new Finder( pattern );
        Files.walkFileTree( Paths.get( path ), EnumSet.noneOf( FileVisitOption.class ), maxDepth, finder );
        return finder.search();
    }

    public static List< Path > Search( String pattern, String path )
            throws IOException {

        Finder finder = new Finder( pattern );
        Files.walkFileTree( Paths.get( path ), finder );
        return finder.search();
    }

    /**
     * Unzip it
     *
     * @param zipFile input zip file
     * @param output  zip file output folder
     */
    public static void unZipIt( String zipFile, String output ) throws MojoFailureException {

        byte[] buffer = new byte[ 1024 ];

        try {

            //create output directory is not exists
            File folder = new File( output );
            if ( !folder.exists() ) {
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis =
                    new ZipInputStream( new FileInputStream( zipFile ) );
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while ( ze != null ) {

                String fileName = ze.getName();
                File newFile = Paths.get( output, fileName ).toFile();
                if ( fileName.endsWith( "/" ) ) {
                    Files.createDirectory( newFile.toPath() );
                } else {
                    System.out.println( "file unzip : " + newFile.getAbsoluteFile() );

                    //create all non exists folders
                    //else you will hit FileNotFoundException for compressed folder
                    boolean mkdirs = new File( newFile.getParent() ).mkdirs();

                    FileOutputStream fos = new FileOutputStream( newFile );

                    int len;
                    while ( ( len = zis.read( buffer ) ) > 0 ) {
                        fos.write( buffer, 0, len );
                    }

                    fos.close();
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch ( IOException ex ) {
            throw new MojoFailureException( ex, "Unzip failed", ex.getMessage() );
        }
    }

}