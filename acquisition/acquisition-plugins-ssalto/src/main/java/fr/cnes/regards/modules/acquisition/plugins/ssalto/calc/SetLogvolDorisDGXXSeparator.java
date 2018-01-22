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

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ICalculationClass;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;

/**
 * Met en forme la version passee en parametre XYYabb sous le format DORIS=X.YY  DIODE=a.bb
 * pour le cas particulier LOGVOL_DORIS_DGXX 
 * 
 * @author Christophe Mertz
 */

public class SetLogvolDorisDGXXSeparator implements ICalculationClass {

    @Override
    public Object calculateValue(Object value, AttributeTypeEnum type, PluginConfigurationProperties properties) {
        String strValue = (String) value;
        return "DORIS=" + strValue.substring(0, 1) + "." + strValue.substring(1, 3) + " - DIODE="
                + strValue.substring(4, 5) + "." + strValue.substring(5, 7);
    }
}
