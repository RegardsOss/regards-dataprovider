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

import java.text.NumberFormat;
import java.util.Locale;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.PluginConfigurationProperties;


/**
 * Cette classe permet de passer de longitude 0 360 a des longitudes
 * -180 180, en limitant la precision a deux chiffres apres la virgule.
 * 
 * @author Christophe Mertz
 *
 */
public class GreenwichCenterLongitudeCalculation implements ICalculationClass {

    /**
     * 
     * @since 1.2
     * 
     */
    public GreenwichCenterLongitudeCalculation() {
        super();
    }

    @Override
    public Object calculateValue(Object pValue, AttributeTypeEnum pType, PluginConfigurationProperties properties) {
        // value is a Double
        double value = Double.parseDouble(pValue.toString());
        if(value>180) {
            value=-360+value;        
        }
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);
        String output = nf.format(value);
        return new Double(output);
    }

}
