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
package fr.cnes.regards.modules.acquisition.tools;

/**
 * Enumerated value for <b>LAST_MODIFICATION_DATE</b> and <b>FILE_SIZE</b>
 * 
 * @author Christophe Mertz
 *
 */
public final class FileInformationTypeEnum {

    /**
     * A {@link String} value
     */
    private final String name;

    /**
     * LAST_MODIFICATION_DATE value
     */
    public static final FileInformationTypeEnum LAST_MODIFICATION_DATE = new FileInformationTypeEnum(
            "LAST_MODIFICATION_DATE");

    /**
     * FILE_SIZE value
     */
    public static final FileInformationTypeEnum FILE_SIZE = new FileInformationTypeEnum("FILE_SIZE");

    private FileInformationTypeEnum(String fileInfo) {
        name = fileInfo;
    }

    /**
     * Get the {@link FileInformationTypeEnum} corresponding to a {@link String} value
     * @param value the value to get the {@link FileInformationTypeEnum}
     * @return the {@link FileInformationTypeEnum} enumerated value
     */
    public static FileInformationTypeEnum parse(String value) {
        FileInformationTypeEnum returnValue = null;
        if (value.equals(FileInformationTypeEnum.LAST_MODIFICATION_DATE.name)) {
            returnValue = LAST_MODIFICATION_DATE;
        } else if (value.equals(FileInformationTypeEnum.FILE_SIZE.name)) {
            returnValue = FILE_SIZE;
        }
        return returnValue;
    }
}
