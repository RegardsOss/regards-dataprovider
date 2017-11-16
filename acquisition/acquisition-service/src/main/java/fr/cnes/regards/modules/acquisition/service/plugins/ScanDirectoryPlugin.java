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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.File;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaFileDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;

/**
 * A default {@link Plugin} of type {@link IAcquisitionScanDirectoryPlugin}.
 *
 * @author Christophe Mertz
 */
@Plugin(id = "ScanDirectoryPlugin", version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect incoming data files", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ScanDirectoryPlugin extends AbstractAcquisitionScanPlugin implements IAcquisitionScanDirectoryPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanDirectoryPlugin.class);

    @Autowired
    private IMetaFileService metaFileService;

    @PluginParameter(name = CHAIN_GENERATION_PARAM, optional = true)
    private String chainLabel;

    @PluginParameter(name = LAST_ACQ_DATE_PARAM, optional = true)
    private String lastDateActivation;

    @PluginParameter(name = META_PRODUCT_PARAM, optional = true)
    private MetaProductDto metaProductDto;

    @PluginParameter(name = META_FILE_PARAM, optional = true)
    private SetOfMetaFileDto metaFiles;

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles() {
        LOGGER.info("Start scan for the chain <{}> ", chainLabel);
        Set<AcquisitionFile> acqFileList = new HashSet<>();

        for (MetaFileDto metaFileDto : metaFiles.getSetOfMetaFiles()) {

            LOGGER.info("Scan Metafile <{}>", metaFileDto.getFileNamePattern());

            MetaFile metaFile = metaFileService.retrieve(metaFileDto.getId());
            scanDirectories(metaFile, acqFileList);
        }

        LOGGER.info("End scan for the chain <{}> ", chainLabel);

        return acqFileList;
    }

    private void scanDirectories(MetaFile metaFile, Set<AcquisitionFile> acqFileList) {

        String filePattern = metaFile.getFileNamePattern();
        String adaptedPattern = getAdaptedPattern(filePattern);
        RegexFilenameFilter filter = new RegexFilenameFilter(adaptedPattern, Boolean.TRUE, Boolean.FALSE);

        for (ScanDirectory scanDir : metaFile.getScanDirectories()) {
            LOGGER.info("Scan directory <{}>", scanDir.getScanDir());

            String dirPath = scanDir.getScanDir();
            File dirFile = new File(dirPath);
            // Check if directory exists and is readable
            if (dirFile.exists() && dirFile.isDirectory() && dirFile.canRead()) {
                addMatchedFile(dirFile, scanDir, filter, metaFile, acqFileList);
            }
        }

    }

    private void addMatchedFile(File dirFile, ScanDirectory scanDir, RegexFilenameFilter filter, MetaFile metaFile,
            Set<AcquisitionFile> acqFileList) {

        List<File> filteredFileList = filteredFileList(dirFile, filter, Strings.isNullOrEmpty(lastDateActivation) ? null
                : OffsetDateTime.parse(lastDateActivation));

        for (File baseFile : filteredFileList) {
            AcquisitionFile acqFile = initAcquisitionFile(baseFile, metaFile, metaProductDto.getChecksumAlgorithm());
            acqFile.setAcqDate(OffsetDateTime.ofInstant(Instant.ofEpochMilli(baseFile.lastModified()),
                                                        ZoneId.of("UTC")));
            acqFileList.add(acqFile);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("new file to acquire : {}", acqFile.getFileName());
            }
        }
    }

    @Override
    public Set<File> getBadFiles() {

        Set<File> badFiles = new HashSet<>();

        for (MetaFileDto metaFileDto : metaFiles.getSetOfMetaFiles()) {
            MetaFile metaFile = metaFileService.retrieve(metaFileDto.getId());
            badFiles.addAll(reportBadFile(metaFile));
        }

        return badFiles;
    }

    private Set<File> reportBadFile(MetaFile metaFile) {
        LOGGER.info("Start report bad files for the chain <{}> ", chainLabel);
        Set<File> badFiles = new HashSet<>();

        String filePattern = metaFile.getFileNamePattern();
        String adaptedPattern = getAdaptedPattern(filePattern);
        RegexFilenameFilter filter = new RegexFilenameFilter(adaptedPattern, Boolean.TRUE, Boolean.FALSE);

        // invert the result
        filter.setPatternExclusion(Boolean.TRUE);

        for (ScanDirectory scanDir : metaFile.getScanDirectories()) {

            String dirPath = scanDir.getScanDir();
            File dirFile = new File(dirPath);
            // Check if directory exists and is readable
            if (dirFile.exists() && dirFile.isDirectory() && dirFile.canRead()) {
                File[] fileArray = dirFile.listFiles(filter);

                // Add files to list : filter selects only files
                for (int j = 0; j < fileArray.length; j++) {
                    //   process_.addWarnToReport(msg);
                    LOGGER.info("Unexpected file <{}> for the chain <{}>", fileArray[j].getAbsolutePath(), chainLabel);
                    badFiles.add(fileArray[j]);
                }
            }
        }

        LOGGER.info("End report bad files for the chain <{}> ", chainLabel);
        return badFiles;

    }
}
