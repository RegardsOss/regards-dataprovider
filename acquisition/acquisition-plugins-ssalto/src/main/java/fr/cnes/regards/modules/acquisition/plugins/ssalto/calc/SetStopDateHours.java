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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.calc;

import java.time.OffsetDateTime;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ICalculationClass;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;

/**
 * Calculates a new date from a date parameter :<br> 
 * the calculate date is the date parameter
 * <li>with the hour set to <code>23</code>,
 * <li>the minute set to <code>59</code>,
 * <li>the seconds set to <code>59</code>
 * 
 * @author Christophe Mertz
 *
 */
public class SetStopDateHours implements ICalculationClass {

    @Override
    public Object calculateValue(Object value, AttributeTypeEnum attributeType,
            PluginConfigurationProperties properties) {

        return ((OffsetDateTime) value).withHour(23).withMinute(59).withSecond(59);
    }

}
