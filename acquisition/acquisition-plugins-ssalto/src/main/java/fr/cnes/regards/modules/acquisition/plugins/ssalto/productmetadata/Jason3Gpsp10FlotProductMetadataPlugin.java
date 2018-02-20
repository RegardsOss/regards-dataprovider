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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;

/**
 * Metadata caculation's plugin for Jason3 Gpsp10_FLOT products.
 * The TIME_PERIOD attribute is managed specifically.
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Metadata caculation's plugin for Jason3 Gpsp10_FLOT products",
        id = "Jason3Gpsp10FlotProductMetadataPlugin", version = "1.0.0", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class Jason3Gpsp10FlotProductMetadataPlugin extends AbstractJasonGpsp10FlotProductMetadataPlugin {

    /**
     * JASON3 project name
     */
    private static final String PROJECT_NAME = "JASON3";

    @Override
    protected String getProjectName() {
        return PROJECT_NAME;
    }

    @PluginInit
    private void init() {
        System.out.println("coucou init du plugin");
        // TODO CMZ tester que cyclePath existe et est accessible 
    }

}
