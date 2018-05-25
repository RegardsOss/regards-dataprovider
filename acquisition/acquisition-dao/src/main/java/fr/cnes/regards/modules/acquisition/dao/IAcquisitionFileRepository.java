/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;

/**
 * {@link AcquisitionFile} repository
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
@Repository
public interface IAcquisitionFileRepository
        extends JpaRepository<AcquisitionFile, Long>, JpaSpecificationExecutor<AcquisitionFile> {

    /**
     * Search all acquisition files for specified filters
     * @param state {@link AcquisitionFileState} filter
     * @param fileInfo {@link AcquisitionFileInfo} filter
     * @return
     */
    List<AcquisitionFile> findByStateAndFileInfo(AcquisitionFileState state, AcquisitionFileInfo fileInfo);

    /**
     * Search all acquisition files for the given {@link AcquisitionFileState}
     * @param state {@link AcquisitionFileState}
     * @return {@link AcquisitionFile}s
     */
    List<AcquisitionFile> findByState(AcquisitionFileState state);

    /**
     * Count number of {@link AcquisitionFile} associated to the given {@link AcquisitionFileInfo}
     * and with one of the given {@link AcquisitionFileState}s
     * @param fileInfo {@link AcquisitionFile}
     * @param fileStates {@link AcquisitionFileInfo}
     * @return number of matching {@link AcquisitionFile}
     */
    long countByFileInfoAndStateIn(AcquisitionFileInfo fileInfo, List<AcquisitionFileState> fileStates);

    /**
     * Count number of {@link AcquisitionFile} associated to the given {@link AcquisitionFileInfo}
     * @param fileInfo {@link AcquisitionFile}
     * @return number of matching {@link AcquisitionFile}
     */
    long countByFileInfo(AcquisitionFileInfo fileInfo);

}
