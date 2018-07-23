/*
 * 
 * Autopsy Forensic Browser
 * 
 * Copyright 2018 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.commonfilesearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sleuthkit.autopsy.centralrepository.datamodel.CorrelationCase;
import org.sleuthkit.autopsy.centralrepository.datamodel.EamDb;
import org.sleuthkit.autopsy.centralrepository.datamodel.EamDbException;
import static org.sleuthkit.autopsy.timeline.datamodel.eventtype.ArtifactEventType.LOGGER;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.HashUtility;

/**
 * Provides logic for selecting common files from all data sources and all cases
 * in the Central Repo.
 */
abstract class InterCaseCommonAttributeSearcher extends AbstractCommonAttributeSearcher {

    private final EamDb dbManager;

    /**
     * Implements the algorithm for getting common files across all data sources
     * and all cases. Can filter on mime types conjoined by logical AND.
     *
     * @param filterByMediaMimeType match only on files whose mime types can be
     * broadly categorized as media types
     * @param filterByDocMimeType match only on files whose mime types can be
     * broadly categorized as document types
     *
     * @throws EamDbException
     */
    InterCaseCommonAttributeSearcher(boolean filterByMediaMimeType, boolean filterByDocMimeType) throws EamDbException {
        super(filterByMediaMimeType, filterByDocMimeType);
        dbManager = EamDb.getInstance();
    }

    /**
     * @param artifactInstances all 'common files' in central repo
     * @param commonValues matches must ultimately have appeared in this
     * collection
     * @return collated map of instance counts to lists of matches
     */
    Map<Integer, List<CommonAttributeValue>> gatherIntercaseResults(Map<Integer, String> commonValues, Map<Integer, Integer> commonFileCases) {

        // keyis string of value
        Map<String, CommonAttributeValue> interCaseCommonFiles = new HashMap<>();

        for (int commonAttrId : commonValues.keySet()) {

            String md5 = commonValues.get(commonAttrId);
            if (md5 == null || HashUtility.isNoDataMd5(md5)) {
                continue;
            }
            Map<Long, AbstractFile> fileCache = new HashMap<>();

            try {
                int caseId = commonFileCases.get(commonAttrId);
                CorrelationCase autopsyCrCase = dbManager.getCaseById(caseId);
                final String correlationCaseDisplayName = autopsyCrCase.getDisplayName();
                // we don't *have* all the information for the rows in the CR,
                //  so we need to consult the present case via the SleuthkitCase object
                // Later, when the FileInstanceNodde is built. Therefore, build node generators for now.

                if (interCaseCommonFiles.containsKey(md5)) {
                    //Add to intercase metaData
                    final CommonAttributeValue commonAttributeValue = interCaseCommonFiles.get(md5);
                    
                    AbstractCommonAttributeSearchResult searchResult = new InterCaseCommonAttributeSearchResult(commonAttrId, fileCache);
                    commonAttributeValue.addFileInstanceMetadata(searchResult, correlationCaseDisplayName);

                } else {
                    CommonAttributeValue commonAttributeValue = new CommonAttributeValue(md5);
                    interCaseCommonFiles.put(md5, commonAttributeValue);
                    
                    AbstractCommonAttributeSearchResult searchResult = new InterCaseCommonAttributeSearchResult(commonAttrId, fileCache);
                    commonAttributeValue.addFileInstanceMetadata(searchResult, correlationCaseDisplayName);
                    
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error getting artifact instances from database.", ex); // NON-NLS
            }
        }

        Map<Integer, List<CommonAttributeValue>> instanceCollatedCommonFiles = collateMatchesByNumberOfInstances(interCaseCommonFiles);

        return instanceCollatedCommonFiles;
    }

    protected CorrelationCase getCorrelationCaseFromId(int correlationCaseId) throws EamDbException {
        for (CorrelationCase cCase : this.dbManager.getCases()) {
            if (cCase.getID() == correlationCaseId) {
                return cCase;
            }
        }
        throw new IllegalArgumentException("Cannot locate case.");
    }
}