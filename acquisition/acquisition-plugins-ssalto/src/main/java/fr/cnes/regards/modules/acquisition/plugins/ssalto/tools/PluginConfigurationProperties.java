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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools;

import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.AttributeFinder;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class PluginConfigurationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginConfigurationProperties.class);

    protected Properties pluginProperties;

    protected static final String ORF_FILE_PATH_KEY = "ORF_FILEPATH_PATTERN";

    protected static final String CYCLE_FILE_PATH_KEY = "CYCLE_FILEPATH";

    protected static final String ARCS_FILEPATH_KEY = "ARCS_FILEPATH";

    private static final String SEPARATOR = ";";

    private static final String URL_PROPERTIES = "ssalto/domain/plugins/impl/tools/pluginConfiguration.properties";

    /**
     * filePattern du nom du fichier
     */
    protected String fileNamePattern;

    /**
     * liste des finder
     */
    private SortedMap<Integer, AttributeFinder> finderList;

    /**
     * nom du projet utilisant le fichier properties : JASON, JASON2, ...</br>
     * Les proprietes du fichier properties seront prefixees par le nom du projet.
     */
    private String project;

    public PluginConfigurationProperties() {
        super();
        loadProperties();
    }

    public void setProject(String projectName) {
        project = projectName.toUpperCase();
    }

    public String getCycleFileFilepath() {
        return getPropertyValue(CYCLE_FILE_PATH_KEY);
    }

    public String getArcPath() {
        return getPropertyValue(ARCS_FILEPATH_KEY);
    }

    private void loadProperties() {
        pluginProperties = new Properties();

        try (InputStream stream = PluginConfigurationProperties.class.getClassLoader()
                .getResourceAsStream(URL_PROPERTIES)) {
            pluginProperties.load(stream);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private String getPropertyValue(String value) {
        if (project == null) {
            LOGGER.error("project was not set : JASON, JASON2 ...");
        }
        String propertyName = project + "_" + value;
        String propertyValue = pluginProperties.getProperty(propertyName);
        if (propertyValue == null) {
            LOGGER.error(String.format("Property not found %s in file '%s'", propertyName, URL_PROPERTIES));
        }
        return propertyValue;
    }

    public String[] getOrfFilepath() {
        // test if project was set
        if (project == null) {
            LOGGER.error("project was not set : JASON, JASON2 ...");
        }
        String propertyName = project + "_" + ORF_FILE_PATH_KEY;
        String propertyValue = pluginProperties.getProperty(propertyName);

        if (propertyValue == null) {
            LOGGER.error(String.format("Property not found %s in file '%s'", propertyName, URL_PROPERTIES));
        }
        String[] orfFilePath = propertyValue.split(SEPARATOR);
        return orfFilePath;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String filePattern) {
        fileNamePattern = filePattern;
    }

    /**
     * ajoute un finder standard
     * 
     * @param finder
     */
    public void addFileFinder(AttributeFinder finder) {
        if (finderList == null) {
            finderList = new TreeMap<>();
        }
        finderList.put(new Integer(finder.getOrder()), finder);
    }

    public Collection<AttributeFinder> getFinderList() {
        if (finderList != null) {
            return finderList.values();
        } else {
            return null;
        }
    }

}
