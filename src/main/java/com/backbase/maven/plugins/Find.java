package com.backbase.maven.plugins;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

class Find {

    private static class Finder extends SimpleFileVisitor< Path > {

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
}