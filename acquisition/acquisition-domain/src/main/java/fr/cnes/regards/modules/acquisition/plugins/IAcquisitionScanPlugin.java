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
package fr.cnes.regards.modules.acquisition.plugins;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * @author Christophe Mertz
 *
 */
@PluginInterface(description = "Plugin to detect files to acquire")
public interface IAcquisitionScanPlugin {

    Set<AcquisitionFile> getAcquisitionFiles(String chainLabel, MetaProduct metaProduct, OffsetDateTime lastDateActivation);

    Set<File> getBadFiles(String chainLabel, Set<MetaFile> metaFiles);

}
