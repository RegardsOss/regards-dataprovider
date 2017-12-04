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
package fr.cnes.regards.modules.acquisition.dao;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * {@link ChainGeneration} repository
 *
 * @author Christophe Mertz
 */
@Repository
public interface IChainGenerationRepository extends JpaRepository<ChainGeneration, Long> {

    /**
     * Find a {@link ChainGeneration} by label
     * @param name the {@link ChainGeneration} label to find
     * @return an {@link Optional} {@link ChainGeneration}
     */
    Optional<ChainGeneration> findOneByLabel(String name);

    /**
     * Find a {@link ChainGeneration} by {@link MetaProduct}
     * @param metaProduct the {@link MetaProduct} to find
     * @return the finded {@link ChainGeneration}
     */
    ChainGeneration findByMetaProduct(MetaProduct metaProduct);

    /**
     * Find all the {@link ChainGeneration} tahat are active and not running
     * @return the {@link Set} of finded {@link ChainGeneration} 
     */
    Set<ChainGeneration> findByActiveTrueAndRunningFalse();
}
