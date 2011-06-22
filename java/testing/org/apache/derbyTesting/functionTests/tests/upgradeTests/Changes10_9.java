/*

Derby - Class org.apache.derbyTesting.functionTests.tests.upgradeTests.Changes10_9

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package org.apache.derbyTesting.functionTests.tests.upgradeTests;

import org.apache.derbyTesting.junit.SupportFilesSetup;

import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.derbyTesting.junit.JDBC;


/**
 * Upgrade test cases for 10.9.
 */
public class Changes10_9 extends UpgradeChange
{
    ///////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTANTS
    //
    ///////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // STATE
    //
    ///////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // CONSTRUCTOR
    //
    ///////////////////////////////////////////////////////////////////////////////////

    public Changes10_9(String name)
    {
        super(name);
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // JUnit BEHAVIOR
    //
    ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Return the suite of tests to test the changes made in 10.7.
     * @param phase an integer that indicates the current phase in
     *              the upgrade test.
     * @return the test suite created.
     */
    public static Test suite(int phase) {
        TestSuite suite = new TestSuite("Upgrade test for 10.9");

        suite.addTestSuite(Changes10_9.class);
        return new SupportFilesSetup((Test) suite);
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //
    // TESTS
    //
    ///////////////////////////////////////////////////////////////////////////////////

    /**
     * Make sure that generator-based identity columns don't break upgrade/downgrade.
     */
    public void testIdentity() throws Exception
    {
        Statement s = createStatement();

        switch ( getPhase() )
        {
        case PH_CREATE: // create with old version
            s.execute( "create table t_identity1( a int, b int generated always as identity )" );
            s.execute( "insert into t_identity1( a ) values ( 100 )" );
            vetIdentityValues( s, "t_identity1", 1 );
            break;
            
        case PH_SOFT_UPGRADE: // boot with new version and soft-upgrade
            s.execute( "insert into t_identity1( a ) values ( 200 )" );
            vetIdentityValues( s, "t_identity1", 2 );

            s.execute( "create table t_identity2( a int, b int generated always as identity )" );
            s.execute( "insert into t_identity2( a ) values ( 100 )" );
            vetIdentityValues( s, "t_identity2", 1 );

            break;
            
        case PH_POST_SOFT_UPGRADE: // soft-downgrade: boot with old version after soft-upgrade
            s.execute( "insert into t_identity1( a ) values ( 300 )" );
            vetIdentityValues( s, "t_identity1", 3 );

            s.execute( "insert into t_identity2( a ) values ( 200 )" );
            vetIdentityValues( s, "t_identity2", 2 );

            break;

        case PH_HARD_UPGRADE: // boot with new version and hard-upgrade
            s.execute( "insert into t_identity1( a ) values ( 400 )" );
            vetIdentityValues( s, "t_identity1", 4 );

            s.execute( "insert into t_identity2( a ) values ( 300 )" );
            vetIdentityValues( s, "t_identity2", 3 );

            break;
        }
        
        s.close();
    }
    private void    vetIdentityValues( Statement s, String tableName, int expectedRowCount ) throws Exception
    {
        int     actualRowCount = 0;
        int     lastValue = 0;

        ResultSet   rs = s.executeQuery( "select * from " + tableName + " order by a" );

        while( rs.next() )
        {
            actualRowCount++;
            
            int currentValue = rs.getInt( 2 );
            if ( actualRowCount > 1 )
            {
                assertTrue( currentValue > lastValue );
            }
            lastValue = currentValue;
        }

        assertEquals( expectedRowCount, actualRowCount );
    }


}