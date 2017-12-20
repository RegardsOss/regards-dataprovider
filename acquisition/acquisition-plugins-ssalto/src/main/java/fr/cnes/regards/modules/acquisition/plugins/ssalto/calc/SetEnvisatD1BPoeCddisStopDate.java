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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.calc;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.exception.DateUtilException;
import fr.cnes.regards.modules.acquisition.plugins.ICalculationClass;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;
import fr.cnes.regards.modules.acquisition.tools.DateFormatter;

/**
 * Calculates a new date from a date parameter :<br> 
 * the calculate date is the date parameter plus <code>6</code> days and the hour is set to <code>23h59min59seconds</code>
 * 
 * @author Christophe Mertz
 *
 */
public class SetEnvisatD1BPoeCddisStopDate implements ICalculationClass {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SetEnvisatD1BPoeCddisStopDate.class);

    @Override
    public Object calculateValue(Object value, AttributeTypeEnum type, PluginConfigurationProperties properties) {

        GregorianCalendar calendar = new GregorianCalendar();
        Date stopDate = null;

        Date date;
        try {
            date = DateFormatter.parse((String) value, DateFormatter.XS_DATE_TIME_FORMAT);
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_YEAR, 6);
            calendar.set(Calendar.HOUR, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            stopDate = calendar.getTime();
        } catch (DateUtilException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return DateFormatter.getDateRepresentation(stopDate, DateFormatter.XS_DATE_TIME_FORMAT);

    }

}
