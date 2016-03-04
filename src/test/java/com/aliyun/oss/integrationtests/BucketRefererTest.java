/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.integrationtests;

import static com.aliyun.oss.integrationtests.TestConstants.BUCKET_ACCESS_DENIED_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.BucketReferer;

public class BucketRefererTest extends TestBase {

    private static final boolean DEFAULT_EMPTY_REFERER_ALLOWED = true;
    
    @Test
    public void testNormalSetBucketReferer() {
        final String bucketName = getBucketName("normal");
        final String referer0 = "http://www.aliyun.com";
        final String referer1 = "https://www.aliyun.com";
        final String referer2 = "http://www.*.com";
        final String referer3 = "https://www.?.aliyuncs.com";
        
        try {
            client.createBucket(bucketName);
            
            // Set non-empty referer list
            BucketReferer r = new BucketReferer();
            List<String> refererList = new ArrayList<String>();
            refererList.add(referer0);
            refererList.add(referer1);
            refererList.add(referer2);
            refererList.add(referer3);
            r.setRefererList(refererList);
            client.setBucketReferer(bucketName, r);
            
            waitForCacheExpiration(5);
            
            r = client.getBucketReferer(bucketName);
            List<String> returedRefererList = r.getRefererList();
            Assert.assertTrue(r.isAllowEmptyReferer());
            Assert.assertTrue(returedRefererList.contains(referer0));
            Assert.assertTrue(returedRefererList.contains(referer1));
            Assert.assertTrue(returedRefererList.contains(referer2));
            Assert.assertTrue(returedRefererList.contains(referer3));
            Assert.assertEquals(4, returedRefererList.size());
            
            // Set empty referer list
            r.clearRefererList();
            client.setBucketReferer(bucketName, r);
            
            r = client.getBucketReferer(bucketName);
            returedRefererList = r.getRefererList();
            Assert.assertTrue(r.isAllowEmptyReferer());
            Assert.assertEquals(0, returedRefererList.size());
            
            // Referer list not allowed to be empty
            refererList.clear();
            refererList.add(referer0);
            refererList.add(referer3);
            r.setRefererList(refererList);
            r.setAllowEmptyReferer(false);
            client.setBucketReferer(bucketName, r);
            
            r = client.getBucketReferer(bucketName);
            returedRefererList = r.getRefererList();
            Assert.assertFalse(r.isAllowEmptyReferer());
            Assert.assertTrue(returedRefererList.contains(referer0));
            Assert.assertTrue(returedRefererList.contains(referer3));
            Assert.assertEquals(2, returedRefererList.size());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            client.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalSetBucketReferer() {
        final String bucketName = getBucketName("unormal");
        final String referer0 = "http://www.aliyun.com";
        final String referer1 = "https://www.aliyun.com";
        
        try {
            client.createBucket(bucketName);
            
            BucketReferer r = new BucketReferer();
            List<String> refererList = new ArrayList<String>();
            refererList.add(referer0);
            refererList.add(referer1);
            r.setRefererList(refererList);
            
            // Set non-existent source bucket 
            final String nonexistentBucket = getBucketName("nonexistent-bucket");
            try {                
                client.setBucketReferer(nonexistentBucket, r);
                Assert.fail("Set bucket referer should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
            }
            
            // Set bucket without ownership
            final String bucketWithoutOwnership = "oss";
            try {
                client.setBucketReferer(bucketWithoutOwnership, r);
                Assert.fail("Set bucket referer should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(BUCKET_ACCESS_DENIED_ERR));
            }
            
            // Not allow referer list to be empty but we set it empty on purpose.
            // TODO: Why not failed ?
            try {
                r.setAllowEmptyReferer(false);
                r.clearRefererList();
                client.setBucketReferer(bucketName, r);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
            
        } finally {
            client.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalGetBucketReferer() {
        // Get non-existent bucket
        final String nonexistentBucket = getBucketName("non-exist");
        try {
            client.getBucketReferer(nonexistentBucket);
            Assert.fail("Get bucket referer should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }
        
        // Get bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            client.getBucketReferer(bucketWithoutOwnership);
            Assert.fail("Get bucket referer should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(BUCKET_ACCESS_DENIED_ERR));
        }
        
        // Get bucket without setting referer list
        final String bucketWithoutRefererRule = getBucketName("bucket-without-referer");
        try {
            client.createBucket(bucketWithoutRefererRule);
            
            BucketReferer r = client.getBucketReferer(bucketWithoutRefererRule);
            Assert.assertEquals(DEFAULT_EMPTY_REFERER_ALLOWED, r.isAllowEmptyReferer());
            Assert.assertEquals(0, r.getRefererList().size());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            client.deleteBucket(bucketWithoutRefererRule);
        }
    }
}