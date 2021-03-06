/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

/**
 *
 */
public class PartitionedPersistentObjectStoreTestCase extends AbstractMuleTestCase
{

    public static final String OBJECT_KEY = "key";
    public static final String OBJECT_BASE_VALUE = "value";
    private MuleContext mockMuleContext = Mockito.mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private PartitionedPersistentObjectStore<Serializable> os;
    private int numberOfPartitions = 3;

    @Before
    public void setUpMockMuleContext() throws IOException
    {
        numberOfPartitions = 3;
        when(mockMuleContext.getConfiguration().getWorkingDirectory()).thenReturn(".");
        when(mockMuleContext.getExecutionClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        os = new PartitionedPersistentObjectStore<Serializable>(mockMuleContext);
        File objectStorePersistDir = new File(PartitionedPersistentObjectStore.OBJECT_STORE_DIR);
        if (objectStorePersistDir.exists())
        {
            FileUtils.deleteDirectory(objectStorePersistDir);
        }
    }

    @Test
    public void defaultPartitionAndNamedPartitionsDoNotCollide() throws Exception
    {
        openPartitions();
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
        assertAllValuesExistsInPartitionAreUnique(OBJECT_KEY, OBJECT_BASE_VALUE);
    }

    @Test
    public void removeEntries() throws Exception
    {
        openPartitions();
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
        removeEntriesInPartitions();
        assertAllPartitionsAreEmpty();
    }

    @Test(expected = ObjectAlreadyExistsException.class)
    public void storeSameKeyThrowsException() throws Exception
    {
        numberOfPartitions = 0;
        openPartitions();
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
    }

    @Test
    public void objectStorePersistDataBetweenOpenAndClose() throws ObjectStoreException
    {
        openPartitions();
        storeInPartitions(OBJECT_KEY, OBJECT_BASE_VALUE);
        closePartitions();
        openPartitions();
        assertAllValuesExistsInPartitionAreUnique(OBJECT_KEY, OBJECT_BASE_VALUE);
    }

    @Test
    public void allowsAnyPartitionName() throws Exception
    {
        os.open("asdfsadfsa#$%@#$@#$@$%$#&8******ASDFWER??!?!");
    }

    private void closePartitions() throws ObjectStoreException
    {
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.close(getPartitionName(i));
        }
        os.close();
    }

    private void assertAllPartitionsAreEmpty() throws ObjectStoreException
    {
        assertThat(os.contains(OBJECT_KEY), is(false));
        assertThat(os.allKeys().size(),is(0));
        for (int i = 0; i < numberOfPartitions; i++)
        {
            assertThat(os.contains(OBJECT_KEY,getPartitionName(i)), is(false));
            assertThat(os.allKeys(getPartitionName(i)).size(),is(0));
        }
    }

    private void removeEntriesInPartitions() throws ObjectStoreException
    {
        os.remove(OBJECT_KEY);
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.remove(OBJECT_KEY, getPartitionName(i));
        }
    }

    private void assertAllValuesExistsInPartitionAreUnique(String key, String value) throws ObjectStoreException
    {
        assertThat((String) os.retrieve(key), is(value));
        for (int i = 0; i < numberOfPartitions; i++)
        {
            assertThat((String) os.retrieve(key,getPartitionName(i)), is(value + i));
        }
    }

    private void storeInPartitions(String key, String value) throws ObjectStoreException
    {
        os.store(key,value);
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.store(key, value + i, getPartitionName(i));
        }
    }

    private void openPartitions() throws ObjectStoreException
    {
        os.open();
        for (int i = 0; i < numberOfPartitions; i++)
        {
            os.open(getPartitionName(i));
        }
    }

    private String getPartitionName(int i)
    {
        return "partition" + i;
    }

}
