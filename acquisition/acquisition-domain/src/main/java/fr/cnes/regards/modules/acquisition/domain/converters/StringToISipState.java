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
package fr.cnes.regards.modules.acquisition.domain.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.web.bind.annotation.ControllerAdvice;

import fr.cnes.regards.modules.acquisition.domain.SipStateManager;
import fr.cnes.regards.modules.ingest.domain.entity.ISipState;

/**
 * Implement the type conversion logic for a String to a {@link ISipState}.<br>
 * This is automatically used by Spring if need be.
 *
 * @author Sébastien Binda
 */
@ControllerAdvice
public class StringToISipState implements Converter<String, ISipState> {

    @Override
    public ISipState convert(String pSource) {
        return SipStateManager.fromName(pSource);
    }

}