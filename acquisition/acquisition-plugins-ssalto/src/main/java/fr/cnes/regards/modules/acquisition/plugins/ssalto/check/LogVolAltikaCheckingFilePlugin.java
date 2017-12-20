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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.check;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.Product;

/**
 * Manage LOG_VOL_ALTIKA data.<br>
 * This {@link Plugin} checks that the file exists and is accessible.<br>
 * The {@link Product} name is the file name less the extension file.<br>
 * 
 * @author Christophe Mertz
 *
 */
@Plugin(description = "LogVolAltikaCheckingFilePlugin", id = "LogVolAltikaCheckingFilePlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class LogVolAltikaCheckingFilePlugin extends CheckingFilePluginHelper {

    /**
     * The extension file HDR
     */
    private static String EXTENSION_HDR = "_HDR";

    /**
     * The extension file BIN
     */
    private static String EXTENSION_BIN = "_BIN";

    /**
     * The extension file TCH
     */
    private static String EXTENSION_TCH = "_TCH";

    private LogVolAltikaCheckingFilePlugin() {
        super();
        initExtensionList();
    }

    /**
     * Initialize the extension file to remove from the file name
     */
    private void initExtensionList() {
        extensionList.add(EXTENSION_BIN);
        extensionList.add(EXTENSION_HDR);
        extensionList.add(EXTENSION_TCH);
    }
}
