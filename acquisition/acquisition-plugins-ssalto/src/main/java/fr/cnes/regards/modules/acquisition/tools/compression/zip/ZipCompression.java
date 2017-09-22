/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.tools.compression.zip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.tools.compression.AbstractRunnableCompression;
import fr.cnes.regards.modules.acquisition.tools.compression.CompressManager;
import fr.cnes.regards.modules.acquisition.tools.compression.CompressionTypeEnum;
import fr.cnes.regards.modules.acquisition.tools.compression.exception.CompressionException;
import fr.cnes.regards.modules.acquisition.tools.compression.exception.FileAlreadyExistException;

/**
 * Classe specialisee dans la compression de fichiers au format ZIP.
 * </br>Elle prend en compte les particularites de l'outil de compression ZIP.
 * 
 * @author Christophe Mertz
 *
 */
public class ZipCompression extends AbstractRunnableCompression {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipCompression.class);

    /**
     * extension a ajouter
     */
    private static final String ZIP_EXTENSION = ".zip";

    /**
     * Motif representant un fichier avec extension .zip modulo la casse.
     */
    private static final String ZIP_PATTERN = ".*((\\.)(?i)zip)$";

    /**
     * Separateur dans path des fichiers tar, on n'utilise pas le seprarateur systeme pour le rendre independant d'une
     * plateforme, le seprarateur unix permettant une plus grande compatibilite.
     */
    private static final String ZIP_PATH_SEPARATOR = "/";

    /**
     * Tampon d'ecriture
     */
    private static final int BUFFER = 1024;

    @Override
    protected CompressManager runCompress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, Charset pCharset, CompressManager pCompressManager) throws CompressionException {
        // if the file has no zip extension, we add one.
        Pattern pat = Pattern.compile(ZIP_PATTERN);
        File compressedFile = null;
        if (pat.matcher(pCompressedFile.getName()).matches()) {
            compressedFile = new File(pCompressedFile.getParentFile(), pCompressedFile.getName());
        } else {
            // Create a new file with .zip extension
            compressedFile = new File(pCompressedFile.getParentFile(), pCompressedFile.getName() + ZIP_EXTENSION);
        }

        if (compressedFile.exists()) {
            throw new FileAlreadyExistException(
                    String.format("File '%s' already exists", compressedFile.getAbsoluteFile().getName()));
        }

        // Calculate full size
        long totalSize = 0;
        long percentage = 0;
        long compressedSize = 0;

        // Eliminate all files having the same name appearing more than once
        // in the list since ZIP does not accept
        List<File> listWithoutDouble = new ArrayList<>();
        for (File aFile : pFileList) {
            if (!containsFile(listWithoutDouble, aFile)) {
                listWithoutDouble.add(aFile);
                totalSize += aFile.length();
            }
        }

        // Write in this file
        try {
            // Prepare streams
            FileOutputStream dest = new FileOutputStream(compressedFile);
            CheckedOutputStream checksum = new CheckedOutputStream(dest, new Adler32());
            ZipArchiveOutputStream out = new ZipArchiveOutputStream(new BufferedOutputStream(checksum));
            out.setFallbackToUTF8(true);
            if (pCharset != null) {
                out.setEncoding(pCharset.name());
                out.setFallbackToUTF8(true);
            }
            try {
                out.setMethod(ZipOutputStream.DEFLATED);
                out.setLevel(Deflater.BEST_COMPRESSION);

                // List Files in list pFilesList and add them
                for (File fileNow : listWithoutDouble) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Adding '%s' file to '%s' file",
                                                   pathToRootDir(fileNow, pRootDirectory),
                                                   compressedFile.getAbsoluteFile().getName()));
                    }

                    ZipArchiveEntry entry;
                    if (pFlatArchive) {
                        if (fileNow.isFile()) {
                            entry = new ZipArchiveEntry(fileNow, fileNow.getName());
                            out.putArchiveEntry(entry);
                        }
                    } else {
                        entry = new ZipArchiveEntry(fileNow, pathToRootDir(fileNow, pRootDirectory));
                        out.putArchiveEntry(entry);
                    }

                    if (fileNow.isFile()) {

                        try (FileInputStream fi = new FileInputStream(fileNow);
                                BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);) {
                            int count = 0;
                            byte data[] = new byte[BUFFER];
                            count = origin.read(data);

                            while (count != -1) {
                                compressedSize += count;
                                out.write(data, 0, count);
                                count = origin.read(data);
                                percentage = 100 * compressedSize / totalSize;
                                pCompressManager.setPercentage(percentage);
                            }
                        } catch (IOException e) {
                            LOGGER.error("Error copying file " + fileNow.getPath() + "to zip file "
                                    + compressedFile.getPath());
                            throw e;
                        }
                    }

                    out.closeArchiveEntry();

                }
            } finally {
                if (out != null) {
                    out.finish();
                    out.close();
                }
                dest.close();
            }
        } catch (IOException ioE) {
            LOGGER.error(ioE.getMessage());
            throw new CompressionException(
                    String.format("IO error during %s compression", CompressionTypeEnum.ZIP.toString()), ioE);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("The file '%s' is done", compressedFile.getName()));
        }

        pCompressManager.setCompressedFile(compressedFile);

        return pCompressManager;
    }

    /**
     * Permet de calculer le chemin relatif d'un fichier par rapport à un répertoire de plus haut niveau.
     * 
     * @param pFile
     *            le fichier
     * @param pRootDir
     *            le répertoire racine.
     * @return une chaine sous forme de <code>String</code> contenant le chemin relatif.
     */
    private String pathToRootDir(File pFile, File pRootDir) {

        File temporaryFile = new File(pFile.getAbsolutePath());

        StringBuilder pathToRoot = new StringBuilder();
        pathToRoot.insert(0, temporaryFile.getName());

        boolean loop = !temporaryFile.getParentFile().getAbsolutePath().equals(pRootDir.getAbsolutePath());

        // @DM SIPNG-DM-0020-CN : we add / only if the path is not made only by the file name
        if (loop) {
            // we add / only if the path is not made only by the file name
            pathToRoot.insert(0, ZIP_PATH_SEPARATOR);
        }

        while (loop) {
            pathToRoot.insert(0, temporaryFile.getParentFile().getName());

            temporaryFile = temporaryFile.getParentFile();

            loop = !temporaryFile.getParentFile().getAbsolutePath().equals(pRootDir.getAbsolutePath());
            if (loop) {
                // do not add file separator at the start of the path
                pathToRoot.insert(0, ZIP_PATH_SEPARATOR);
            }
        }

        return pathToRoot.toString();
    }

    /**
     * Cette méthode permet de savoir si plusieurs fichiers ont le même nom dans une liste. L'utilité de cette méthode
     * est de s'assurer que 2 fichiers à zipper n'ont pas les mêmes noms car cela fait planter la compression.
     * 
     * @param pFilesLst
     *            la liste de fichiers
     * @param pFile
     *            le fichier
     * @return a {@link Boolean}
     */
    private boolean containsFile(List<File> pFilesLst, File pFile) {
        boolean contains = false;

        for (File file : pFilesLst) {
            if (!contains && pFile.getPath().equals(file.getPath())) {
                contains = true;
            }
        }

        return contains;
    }

    /**
     * Permet de decompresser un fichier compresse dans un repertoire cible
     * 
     * @param pCompressedFile
     *            le fichier a decompresser
     * @param pOutputDir
     *            le repertoire destination
     * @throws CompressionException
     *             si l'un des paramètres est incorrect ou illisible
     * @see sipad.tools.compression.ICompression#uncompress(java.io.File, java.io.File)
     */
    @Override
    public void uncompress(File pCompressedFile, File pOutputDir) throws CompressionException {
        uncompress(pCompressedFile, pOutputDir, null);
    }

    /**
     * Permet de decompresser un fichier compresse dans un repertoire cible
     * 
     * @param pCompressedFile
     *            le fichier a decompresser
     * @param pOutputDir
     *            le repertoire destination
     * @throws CompressionException
     *             si l'un des paramètres est incorrect ou illisible
     */
    @SuppressWarnings("resource")
    @Override
    public void uncompress(File pCompressedFile, File pOutputDir, Charset pCharset) throws CompressionException {

        // pCompressedFile must have .zip extension
        if (!pCompressedFile.getName().toLowerCase().endsWith(ZIP_EXTENSION)) {
            throw new CompressionException(String.format("Extension must be '%s'", ZIP_EXTENSION));
        }

        try {
            // prepare buffer and streams
            byte data[] = new byte[BUFFER];
            FileInputStream fis = new FileInputStream(pCompressedFile);
            BufferedInputStream buffi = new BufferedInputStream(fis);
            ZipInputStream zis = null;
            ZipEntry entry = null;

            if (pCharset != null) {
                zis = new ZipInputStream(buffi, pCharset);
            } else {
                zis = new ZipInputStream(buffi);
            }

            // List compressed files in ZIP and create them if not exist
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(pOutputDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!newFile.exists()) {
                        Files.createDirectory(Paths.get(newFile.getAbsolutePath()));
                    }
                } else {

                    if (newFile.exists()) {
                        throw new FileAlreadyExistException(
                                String.format("File '%s' already exists", newFile.getName()));
                    }

                    if (newFile.getParentFile() != null && !newFile.getParentFile().exists()) {
                        Files.createDirectory(Paths.get(newFile.getParentFile().getAbsolutePath()));
                    }

                    FileOutputStream fos = new FileOutputStream(newFile);
                    try (BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER)) {
                        int count = 0;
                        while ((count = zis.read(data, 0, BUFFER)) != -1) {
                            dest.write(data, 0, count);
                        }
                        dest.flush();
                    }
                    fos.close();
                }
            }
            fis.close();
            zis.close();
        } catch (IOException ioE) {
            throw new CompressionException(
                    String.format("IO error during %s uncompression", CompressionTypeEnum.ZIP.toString()), ioE);
        }
    }

}