/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
    ...
**********************************************************************/
package org.datanucleus.api.jpa;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import org.datanucleus.exceptions.NucleusException;

/**
 * Implementation of a FetchPlan for JPA.
 * Provides a JPA wrapper around the internal org.datanucleus.FetchPlan.
 */
public class JPAFetchPlan implements Serializable
{
    private static final long serialVersionUID = 7955990781412643432L;
    private final org.datanucleus.FetchPlan fp;

    /**
     * Constructor.
     * @param fp FetchPlan
     */
    public JPAFetchPlan(org.datanucleus.FetchPlan fp)
    {
        this.fp = fp;
    }

    /**
     * Accessor for the groups.
     * @return The groups
     */
    public Set<String> getGroups()
    {
        return fp.getGroups();
    }

    /**
     * Method to add a group to the fetch plan.
     * @param group The group to add
     * @return The updated FetchPlan
     */
    public JPAFetchPlan addGroup(String group)
    {
        fp.addGroup(group);
        return this;
    }

    /**
     * Method to clear the fetch plan groups.
     * @return The updated FetchPlan
     */
    public JPAFetchPlan clearGroups()
    {
        fp.clearGroups();
        return this;
    }

    /**
     * Method to remove a group from the FetchPlan.
     * @param group The group to remove
     * @return The updated FetchPlan
     */
    public JPAFetchPlan removeGroup(String group)
    {
        fp.removeGroup(group);
        return this;
    }

    /**
     * Method to set the FetchPlan to a single group.
     * @param group The group to set
     * @return The updated FetchPlan
     */
    public JPAFetchPlan setGroup(String group)
    {
        fp.setGroup(group);
        return this;
    }

    /**
     * Method to set the groups to the passed collection.
     * @param groups Collection of groups
     * @return Updated FetchPlan
     */
    public JPAFetchPlan setGroups(Collection groups)
    {
        fp.setGroups(groups);
        return this;
    }

    /**
     * Method to set the groups to the passed array.
     * @param groups Collection of groups
     * @return Updated FetchPlan
     */
    public JPAFetchPlan setGroups(String... groups)
    {
        fp.setGroups(groups);
        return this;
    }

    /**
     * Accessor for the fetch size.
     * @return The fetch size
     */
    public int getFetchSize()
    {
        return fp.getFetchSize();
    }

    /**
     * Method to set the fetch size (large result sets).
     * @param size The size
     * @return Updated FetchPlan
     */
    public JPAFetchPlan setFetchSize(int size)
    {
        fp.setFetchSize(size);
        return this;
    }

    /**
     * Accessor for the max fetch depth.
     * @return Max fetch depth
     */
    public int getMaxFetchDepth()
    {
        return fp.getMaxFetchDepth();
    }

    /**
     * Method to set the max fetch depth.
     * @param depth The depth
     * @return Updated FetchPlan
     */
    public JPAFetchPlan setMaxFetchDepth(int depth)
    {
        try
        {
            fp.setMaxFetchDepth(depth);
        }
        catch (NucleusException jpe)
        {
            throw JPAAdapter.getJPAExceptionForNucleusException(jpe);
        }
        return this;
    }

    /**
     * Accessor for the detachment options.
     * @return Detachment options.
     */
    public int getDetachmentOptions()
    {
        return fp.getDetachmentOptions();
    }

    /**
     * Accessor for the detachment root classes.
     * @return Detachment root classes
     */
    public Class[] getDetachmentRootClasses()
    {
        return fp.getDetachmentRootClasses();
    }

    /**
     * Accessor for the detachment roots.
     * @return Detachment roots
     */
    public Collection getDetachmentRoots()
    {
        return fp.getDetachmentRoots();
    }

    /**
     * Method to set the detachment options.
     * @param options Detachment options
     * @return Updated FetchPlan
     */
    public JPAFetchPlan setDetachmentOptions(int options)
    {
        try
        {
            fp.setDetachmentOptions(options);
        }
        catch (NucleusException jpe)
        {
            throw JPAAdapter.getJPAExceptionForNucleusException(jpe);
        }
        return this;
    }

    /**
     * Method to set the detachment root classes.
     * @param rootClasses The detachment root classes
     * @return Updated FetchPlan
     */
    public JPAFetchPlan setDetachmentRootClasses(Class... rootClasses)
    {
        try
        {
            fp.setDetachmentRootClasses(rootClasses);
        }
        catch (NucleusException jpe)
        {
            throw JPAAdapter.getJPAExceptionForNucleusException(jpe);
        }
        return this;
    }

    /**
     * Method to set the detachment roots.
     * @param roots Detachment roots
     * @return Updated FetchPlan
     */
    public JPAFetchPlan setDetachmentRoots(Collection roots)
    {
        fp.setDetachmentRoots(roots);
        return this;
    }

    /**
     * Accessor for the internal fetch plan.
     * @return Internal fetch plan
     */
    public final org.datanucleus.FetchPlan getInternalFetchPlan()
    {
        return fp;
    }

    public String toString()
    {
        return fp.toString();
    }
}