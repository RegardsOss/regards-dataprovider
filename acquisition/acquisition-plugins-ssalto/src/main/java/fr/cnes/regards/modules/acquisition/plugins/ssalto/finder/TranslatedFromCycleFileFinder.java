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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.finder;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;

/**
 * Cette classe permet de passer d'une date a une occurence de cycle et vice versa. Elle utilise 2 fichiers pour cela
 * voir plusieurs dans un cas particulier. le fichier cycle et le(s) fichier(s) ORF.
 * 
 * @author Christophe Mertz
 *
 */
public class TranslatedFromCycleFileFinder extends OtherAttributeValueFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TranslatedAttributeFromArcFile.class);

    // Charset and decoder_ for ISO-8859-15
    private static Charset charset_ = Charset.forName("ISO-8859-15");

    private final CharsetDecoder decoder_ = charset_.newDecoder();

    /**
     * Pattern de date dans un fichier de cycle
     */
    private static final String CYCLE_DATE_PATTERN = "\\d{2}-\\d{2}-\\d{4}";

    // TODO change les \\s en \\{bla,k} et repasser les tests unitaires
    /**
     * Pattern pour les lignes dans le fichier cycle
     */
    private final Pattern CYCLE_LINE_PATTERN = Pattern
            .compile("-\\s*(\\d{3})\\s*(" + CYCLE_DATE_PATTERN + ")\\s*(-|" + CYCLE_DATE_PATTERN + ")\\s*");

    /**
     * numero du groupe pour le groupe du numero de cycle
     */
    private static final int CYCLE_GROUP = 1;

    /**
     * DM60 - Prise en compte d'un cas particulier. Gestion du cycle lorsque la date est egale ou inferieure a la 1ere
     * date du cycle dans le fichier MISSION_CYCLES Dans ce cas il faut affiner la recherche avec les fichiers ORF
     * numero du groupe pour le groupe de la start date
     */
    private static final int CYCLE_START_GROUP = 2;

    /**
     * numero du groupe pour le groupe de la stop date
     */
    private static final int CYCLE_STOP_GROUP = 3;

    /**
     * Format de la date dans les fichiers cycle
     */
    private final SimpleDateFormat CYCLE_DATE_FORMAT;

    /**
     * Pattern de date dans un fichier orf
     */
    private static final String ORF_DATE_PATTERN = "\\d{4}/\\d{2}/\\d{2}";

    /**
     * Pattern de time dans un fichier de cycle
     */
    private static final String ORF_TIME_PATTERN = "\\d{2}:\\d{2}:\\d{2}.\\d{3}";

    /**
     * Pattern pour les lignes dans le fichier orf
     */
    private final Pattern ORF_LINE_PATTERN = Pattern
            .compile("(" + ORF_DATE_PATTERN + "\\p{Blank}" + ORF_TIME_PATTERN + ")\\p{Blank}(\\d{3}).*");

    /**
     * numero du groupe pour le groupe du time dans le fichier orf
     */
    private static final int ORF_TIME_GROUP = 1;

    /**
     * numero du groupe pour le groupe du numero de cycle
     */
    private static final int ORF_CYCLE_GROUP = 2;

    /**
     * Format de la date dans le fichier orf
     */
    private final SimpleDateFormat ORF_DATE_TIME_FORMAT;

    public TranslatedFromCycleFileFinder() {
        super();
        ORF_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        CYCLE_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    }

    @Override
    public List<Object> getValueList(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        final List<Object> otherValueList = super.getValueList(pFileMap, pAttributeValueMap);
        // la list doit etre de taille 1
        if (otherValueList.size() > 1) {
            final String msg = "Too much value for cycle attribut : " + super.getOtherAttributName();
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }
        // on applique le calcul sur les resultats
        final List<Object> processedValuesList = new ArrayList<>();
        final Object processValue = processCalculOnValues(otherValueList.get(0));
        processedValuesList.add(processValue);
        return processedValuesList;
    }

    /**
     * Calcul l'integer de l'occurence de cycle a partir de la date ou vice versa
     *
     * @param pOtherValue
     *            Date ou Integer
     * @return un Integer correspondant au numero de cycle ou une date correspondant au startDate
     * @throws PluginAcquisitionException
     */
    private Object processCalculOnValues(Object pOtherValue) throws PluginAcquisitionException {
        // Integer or Date
        Object processedValue = null;
        // valueType est la valeur de sortie si c'est integer => on doit trouver
        // le cycle correspondant au start_date et vice versa
        if (valueType.equals(AttributeTypeEnum.TYPE_INTEGER)) {
            final Date startDate = (Date) pOtherValue;

            // Check if cycle file exists
            final String cycleFilePath = confProperties.getCycleFileFilepath();

            boolean computeOrfFile = true;
            if ((cycleFilePath != null) && (cycleFilePath.length() > 0)) {
                final File cycleFile = new File(cycleFilePath);
                if (cycleFile.exists()) {
                    // Compute value from cycle file first and orf file if necessary
                    processedValue = getCycleOcurrence(startDate);
                    computeOrfFile = false;
                }
            }

            if (computeOrfFile) {
                // Compute value from orf file only
                processedValue = getCycleOccurenceFromOrf(startDate);
            }

        } else if (valueType.equals(AttributeTypeEnum.TYPE_DATE)
                || valueType.equals(AttributeTypeEnum.TYPE_DATE_TIME)) {
            final Integer cycleOccurence = (Integer) pOtherValue;
            if (getName().equals("START_DATE")) {
                processedValue = getCycleStartDate(cycleOccurence);
            } else if (getName().equals("STOP_DATE")) {
                processedValue = getCycleStopDate(cycleOccurence);
            }
        } else {
            final String msg = "Attribut Type must be INTEGER or DATE";
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }
        return processedValue;
    }

    /**
     * Retourne la date correspondant à la premiere ligne contenant l'occurence du cycle pCycleOccurence dans le fichier
     * orf.
     *
     * @param pCycleOccurence
     *            l'occurence du cycle.
     * @return
     * @throws PluginAcquisitionException
     */
    private synchronized Date getCycleStartDate(Integer pCycleOccurence) throws PluginAcquisitionException {
        Date cycleStartDate = null;
        int increment = 0;
        String cycleAsString = null;

        // occurence du cycle
        final NumberFormat numberFormat = new DecimalFormat("000");
        cycleAsString = numberFormat.format(pCycleOccurence.intValue());

        // recupere le ou les fichiers depuis le disk
        // DM SIPNG-DM-0060-CN : Prise en compte de x fichiers ORF
        final String[] getOrfFilePath = confProperties.getOrfFilepath();
        while ((cycleStartDate == null) && (increment <= (getOrfFilePath.length - 1))) {
            final CharBuffer fileBuffer = getOrfFile(getOrfFilePath[increment++]);

            // parcours le fichier ligne par ligne
            final Matcher matcher = ORF_LINE_PATTERN.matcher(fileBuffer);
            while (matcher.find() && (cycleStartDate == null)) {
                if (cycleAsString.equals(matcher.group(ORF_CYCLE_GROUP))) {
                    // cree la date pour comparaison
                    try {
                        cycleStartDate = ORF_DATE_TIME_FORMAT.parse(matcher.group(ORF_TIME_GROUP));
                    } catch (final ParseException e) {
                        throw new PluginAcquisitionException(e);
                    }
                }
            }
        }
        // le cycle n'a pas ete trouve
        if (cycleStartDate == null) {
            final String msg = "cycle not found in orf file : " + cycleAsString;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }
        return cycleStartDate;
    }

    /**
     * Retourne la date correspondant à la derniere ligne contenant l'occurence du cycle pCycleOccurence dans le fichier ORF
     *
     * @param pCycleOccurence
     *            l'occurence du cycle.
     * @return
     * @throws PluginAcquisitionException
     */
    private synchronized Date getCycleStopDate(Integer pCycleOccurence) throws PluginAcquisitionException {
        Date cycleStopDate = null;
        int increment = 0;
        String cycleAsString = null;

        // occurence du cycle +1
        final NumberFormat numberFormat = new DecimalFormat("000");
        cycleAsString = numberFormat.format(pCycleOccurence.intValue());

        // recupere le ou les fichiers depuis le disk
        // DM SIPNG-DM-0060-CN : Prise en compte de x fichiers ORF
        final String[] getOrfFilePath = confProperties.getOrfFilepath();
        while ((cycleStopDate == null) && (increment <= (getOrfFilePath.length - 1))) {

            // recupere le fichier depuis le disk
            final CharBuffer fileBuffer = getOrfFile(getOrfFilePath[increment++]);

            // parcours le fichier ligne par ligne
            final Matcher matcher = ORF_LINE_PATTERN.matcher(fileBuffer);
            while (matcher.find()) {
                if (cycleAsString.equals(matcher.group(ORF_CYCLE_GROUP))) {
                    // cree la date pour comparaison
                    try {
                        cycleStopDate = ORF_DATE_TIME_FORMAT.parse(matcher.group(ORF_TIME_GROUP));
                    } catch (final ParseException e) {
                        throw new PluginAcquisitionException(e);
                    }
                }
            }
        }
        // le cycle n'a pas ete trouve
        if (cycleStopDate == null) {
            final String msg = "cycle not found in orf file : " + cycleAsString;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }
        return cycleStopDate;
    }

    /**
     * Le nom des fichiers ORF evolue en fonction des mises a jours. On ne selectionne que le dernier modifie
     *
     * @param orfFilepathPattern_
     *            de preference en chemin absolu
     * @return
     * @throws PluginAcquisitionException
     */
    private CharBuffer getOrfFile(String orfFilepathPattern_) throws PluginAcquisitionException {

        CharBuffer fileBuffer;
        final File dir = new File(new File(orfFilepathPattern_).getParent());
        final String filePattern = new File(orfFilepathPattern_).getName();
        final FileFilter fileFilter = new WildcardFileFilter(filePattern);
        final File[] files = dir.listFiles(fileFilter);
        // recupere le dernier fichier modifier
        final SortedSet<File> fileSet = new TreeSet<>(LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        // doit contenir au moins un fichier
        if (files == null) {
            final String msg = "No file found in dir " + dir.getAbsolutePath() + " for filePattern " + filePattern;
            throw new PluginAcquisitionException(msg);
        } else {
            for (final File file : files) {
                fileSet.add(file);
            }
        }
        // doit contenir au moins un fichier
        if (fileSet.isEmpty()) {
            final String msg = "No file found in dir " + dir.getAbsolutePath() + " for filePattern " + filePattern;
            throw new PluginAcquisitionException(msg);
        } else {
            try {
                final File lastModifiedFile = fileSet.last();
                fileBuffer = readFile(lastModifiedFile.getAbsolutePath());
            } catch (final IOException e) {
                throw new PluginAcquisitionException(e);
            }
        }
        return fileBuffer;
    }

    /**
     * Retourne l'occurence du cycle correpondant a la Date
     *
     * @param pStartDate
     *            la date pour laquelle on cherche le cycle correspondant
     * @return
     * @throws PluginAcquisitionException
     */
    public synchronized Integer getCycleOcurrence(Date pStartDate) throws PluginAcquisitionException {
        Integer cycleOccurence = null;
        int firstStartDate = 0;

        // recupere le fichier depuis le disk
        CharBuffer fileBuffer = null;
        try {
            fileBuffer = readFile(confProperties.getCycleFileFilepath());
        } catch (final IOException e) {
            throw new PluginAcquisitionException(new File(confProperties.getCycleFileFilepath()).getAbsolutePath(), e);
        }

        // convertit la date pour la comparaison
        final String stringpStartDate = CYCLE_DATE_FORMAT.format(pStartDate);
        // parcours le fichier ligne par ligne
        final Matcher matcher = CYCLE_LINE_PATTERN.matcher(fileBuffer);
        // look in ORF file
        boolean orfCycle = false;
        while (matcher.find() && (cycleOccurence == null)) {

            if (firstStartDate == 0) {
                // Si la date passee en parametre est identique ou
                // inferieure a la PREMIERE date de debut du premier cycle
                // alors affiner la recherche en cherchant parmi les
                // fichiers ORF ( cas des cycles 000 pour jason ou 998 et
                // 999 pour jason2 )
                final String startDateString = matcher.group(CYCLE_START_GROUP);

                firstStartDate = 1;

                Date startDate = null;
                try {
                    startDate = CYCLE_DATE_FORMAT.parse(startDateString);
                } catch (final ParseException e) {
                    throw new PluginAcquisitionException(e);
                }
                if (pStartDate.before(startDate) || pStartDate.equals(startDate)) {
                    cycleOccurence = getCycleOccurenceFromOrf(pStartDate);
                    orfCycle = true;
                }
            }

            if (!orfCycle) {

                // si la date cherchee est superieur a la stopDate on passe
                // a la
                // ligne suivante
                final String stopDateString = matcher.group(CYCLE_STOP_GROUP);

                // cas du dernier cycle
                if (stopDateString.equals("-")) {
                    cycleOccurence = new Integer(matcher.group(CYCLE_GROUP));
                } else if (stopDateString.equals(stringpStartDate)) {
                    // Les dates sont egales sans la precision des heures
                    // il faut regarder le 2eme fichier
                    cycleOccurence = getCycleOccurenceFromOrf(pStartDate);
                } else {

                    Date stopDate = null;
                    try {
                        stopDate = CYCLE_DATE_FORMAT.parse(stopDateString);
                    } catch (final ParseException e) {
                        throw new PluginAcquisitionException(e);
                    }
                    if (pStartDate.after(stopDate)) {
                        // rien a faire
                    } else {
                        // La date est entre la stopDate de la ligne
                        // precedente =
                        // startDate de la ligne courante
                        // et la stopDate de la ligne courante
                        cycleOccurence = new Integer(matcher.group(CYCLE_GROUP));
                    }
                }
            }
        }

        return cycleOccurence;
    }

    /**
     * Retrouve dans le fichier Orf l'occurence du cycle correspondant a la date
     *
     * @param pStartDate
     * @return l'occurence du cycle correspondant a la date
     * @throws PluginAcquisitionException
     */
    public synchronized Integer getCycleOccurenceFromOrf(Date pStartDate) throws PluginAcquisitionException {
        Integer cycleOccurence = null;
        int increment = 0;
        // indique que le bon cycle occurence a ete trouve
        boolean cycleFound = false;

        // recupere le fichier depuis le disk
        // DM-0060 : Prise en compte de plusieurs fichiers ORF
        final String[] getOrfFilePath = confProperties.getOrfFilepath();
        while ((!cycleFound) && (increment <= (getOrfFilePath.length - 1))) {

            final CharBuffer fileBuffer = getOrfFile(getOrfFilePath[increment++]);
            // parcours le fichier ligne par ligne
            final Matcher matcher = ORF_LINE_PATTERN.matcher(fileBuffer);
            while (matcher.find() && !cycleFound) {

                // cree la date pour comparaison
                Date startDate = null;
                try {
                    startDate = ORF_DATE_TIME_FORMAT.parse(matcher.group(ORF_TIME_GROUP));
                } catch (final ParseException e) {
                    throw new PluginAcquisitionException(e);
                }
                // Si la date trouvee est anterieure on stocke l'occurence sinon
                // c'est la fin
                if (startDate.before(pStartDate)) {
                    // sauvegarde l'occurence
                    cycleOccurence = new Integer(matcher.group(ORF_CYCLE_GROUP));
                } else {
                    // dans le cas ou la pStartDate est anterieure a la premiere
                    // occurrence d'ORF
                    // on prend la 1ere
                    if (cycleOccurence == null) {
                        cycleOccurence = new Integer(matcher.group(ORF_CYCLE_GROUP));
                    }
                    cycleFound = true;
                }
            }
        }
        return cycleOccurence;
    }

    /**
     * Lit un fichier sous forme de charBuffer.
     *
     * @param pFile
     * @return
     * @throws IOException
     */
    @SuppressWarnings("resource")
    private CharBuffer readFile(String pFilepath) throws IOException {

        // Open the file and then get a channel from the stream
        final FileInputStream fis = new FileInputStream(pFilepath);
        final FileChannel fc = fis.getChannel();

        // Get the file's size and then map it into memory
        final int sz = (int) fc.size();
        final MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

        // Decode the file into a char buffer
        final CharBuffer cb = decoder_.decode(bb);

        // Close File
        fis.close();

        return cb;
    }

    protected Object getTranslatedValue(Object pvalue) throws PluginAcquisitionException {
        return processCalculOnValues(pvalue);
    }
}
