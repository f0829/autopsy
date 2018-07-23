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

import java.util.Map;
import org.sleuthkit.autopsy.centralrepository.datamodel.CorrelationAttributeInstance;
import org.sleuthkit.autopsy.datamodel.DisplayableItemNode;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.TskCoreException;

/**
 * Defines leaf-type nodes used in the Common Files Search results tree. Leaf
 * nodes, may describe common attributes which exist in the current case DB or
 * in the Central Repo. When a reference to the AbstractFile is lacking (such as
 * in the case that a common attribute is found in the Central Repo) not all
 * features of the Content Viewer, and context menu can be supported. Thus,
 * multiple types of leaf nodes are required to represent Common Attribute
 * Instance nodes.
 */
public abstract class AbstractCommonAttributeSearchResult {

    private final Long abstractFileObjectId;
    // maps object ID to file
    private final Map<Long, AbstractFile> cachedFiles;
    private final String caseName;
    private final String dataSource;

    /**
     * Create a leaf node for attributes found in files in the current case db.
     *
     * @param abstractFileReference file from which the common attribute was
     * found
     * @param cachedFiles storage for abstract files which have been used
     * already so we can avoid extra roundtrips to the case db
     * @param dataSource datasource where this attribute appears
     * @param caseName case where this attribute appears
     */
    AbstractCommonAttributeSearchResult(Long abstractFileReference, Map<Long, AbstractFile> cachedFiles, String dataSource, String caseName) {
        this.abstractFileObjectId = abstractFileReference;
        this.cachedFiles = cachedFiles;
        this.caseName = caseName;
        this.dataSource = dataSource;
    }

    /**
     * Create a leaf node for attributes found in the central repo and not
     * available in the current data case.
     *
     * @param cachedFiles storage for abstract files which have been used
     * already so we can avoid extra roundtrips to the case db
     */
    AbstractCommonAttributeSearchResult(Map<Long, AbstractFile> cachedFiles) {
        this.abstractFileObjectId = -1L;
        this.cachedFiles = cachedFiles;
        this.caseName = "";
        this.dataSource = "";
    }

    /**
     * Grab a cached instance of the AbstractFile or grab one from the
     * SleuthkitCase. Use this in implementations of <code>generateNodes</code>.
     *
     * @return AbstractFile which is identical to the file instance generated by
     * implementations of this object
     */
    AbstractFile lookupOrLoadAbstractFile() {
        if (this.cachedFiles.containsKey(this.getAbstractFileObjectId())) {
            return this.cachedFiles.get(this.getAbstractFileObjectId());
        } else {
            AbstractFile file = this.loadFileFromSleuthkitCase();
            final long abstractFileId = file.getId();
            this.cachedFiles.put(abstractFileId, file);
            return file;
        }
    }

    /**
     * Implement this in subclasses to find the AbstractFile by whatever means
     * available. This will be called by this.lookupOrLoadAbstractFile. In some
     * cases we may have the object abstractFileId, in other cases we may need 
     * to use the file name.
     *
     * @return AbstractFile corresponding to this common attribute
     */
    abstract AbstractFile loadFileFromSleuthkitCase();

    /**
     * Create a list of leaf nodes, to be used to display a row in the tree
     * table
     *
     * @return leaf nodes for tree
     */
    abstract DisplayableItemNode[] generateNodes();

    /**
     * The name of the case where this common attribute is found.
     *
     * @return case name
     */
    String getCaseName() {
        return this.caseName;
    }

    /**
     * Get string name of the data source where this common attribute appears.
     *
     * @return data source name
     */
    public String getDataSource() {

        /**
         * Even though we could get this from the CR record or the AbstractFile,
         * we want to avoid getting it from the AbstractFile because it would be
         * an extra database roundtrip.
         */
        return this.dataSource;
    }

    /**
     * ObjectId of the AbstractFile that is equivalent to the file from which
     * this common attribute instance
     *
     * @return the abstractFileObjectId
     */
    public Long getAbstractFileObjectId() {
        return abstractFileObjectId;
    }

    /**
     * Use this to create an AbstractCommonAttributeInstanceNode of the
     * appropriate type. In any case, we'll get something which extends
     * DisplayableItemNode which can be used to populate the tree.
     *
     * If the common attribute in question could be derived from an AbstractFile
     * in the present SleuthkitCase, we can use an
     * IntraCaseCommonAttributeInstanceNode which enables extended functionality
     * in the context menu and in the content viewer.
     *
     * Otherwise, we will get an InterCaseCommonAttributeInstanceNode which
     * supports only baseline functionality.
     *
     * @param attributeInstance common file attribute instance form the central
     * repo
     * @param equivalentAbstractFile an AbstractFile which is equivalent to the
     * file in which the attribute instance was found
     * @param currentCaseName
     * @return the appropriate leaf node for the results tree
     * @throws TskCoreException
     */
    static DisplayableItemNode createInstance(CorrelationAttributeInstance attributeInstance, AbstractFile equivalentAbstractFile, String currentCaseName) throws TskCoreException {

        DisplayableItemNode leafNode;

        final String attributeDataSourceName = attributeInstance.getCorrelationDataSource().getName();
        final String abstractFileDataSourceName = equivalentAbstractFile.getDataSource().getName();

        final String attributeInstanceCaseName = attributeInstance.getCorrelationCase().getDisplayName();

        final String attributeInstanceFullPath = attributeInstance.getFilePath().replace("\\", "/");

        final String currentAbstractFileParentPath = equivalentAbstractFile.getParentPath();
        final String currentAbstractFileName = equivalentAbstractFile.getName();
        final String abstractFileFullPath = (currentAbstractFileParentPath + currentAbstractFileName).replace("\\", "/");

        final boolean sameCase = attributeInstanceCaseName.equalsIgnoreCase(currentCaseName);
        final boolean sameFileName = attributeInstanceFullPath.equalsIgnoreCase(abstractFileFullPath);
        final boolean sameDataSource = attributeDataSourceName.equalsIgnoreCase(abstractFileDataSourceName);

        if (sameCase && sameFileName && sameDataSource) {
            leafNode = new IntraCaseCommonAttributeInstanceNode(equivalentAbstractFile, currentCaseName, abstractFileDataSourceName);
        } else {
            leafNode = new InterCaseCommonAttributeInstanceNode(attributeInstance, equivalentAbstractFile);
        }
        return leafNode;
    }
}