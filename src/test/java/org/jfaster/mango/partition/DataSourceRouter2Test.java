/*
 * Copyright 2014 mango.jfaster.org
 *
 * The Mango Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.jfaster.mango.partition;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.jfaster.mango.annotation.DB;
import org.jfaster.mango.annotation.ReturnGeneratedId;
import org.jfaster.mango.annotation.SQL;
import org.jfaster.mango.annotation.ShardBy;
import org.jfaster.mango.datasource.DataSourceFactory;
import org.jfaster.mango.datasource.MultipleDataSourceFactory;
import org.jfaster.mango.datasource.SimpleDataSourceFactory;
import org.jfaster.mango.operator.Mango;
import org.jfaster.mango.support.DataSourceConfig;
import org.jfaster.mango.support.Randoms;
import org.jfaster.mango.support.Table;
import org.jfaster.mango.support.model4table.Msg;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * 测试数据源路由
 *
 * @author ash
 */
public class DataSourceRouter2Test {

    private static Mango mango;
    private static String[] dsns = new String[] {"ds1", "ds2", "ds3"};

    @Before
    public void before() throws Exception {
        Map<String, DataSourceFactory> factories = new HashMap<String, DataSourceFactory>();
        for (int i = 0; i < 3; i++) {
            DataSource ds = DataSourceConfig.getDataSource(i + 1);
            Connection conn = ds.getConnection();
            Table.MSG.load(conn);
            conn.close();
            factories.put(dsns[i], new SimpleDataSourceFactory(ds));
        }
        DataSourceFactory dsf = new MultipleDataSourceFactory(factories);
        mango = Mango.newInstance(dsf);
    }

    @Test
    public void testRandomPartition() {
        MsgDao dao = mango.create(MsgDao.class);
        int num = 100;
        List<Msg> msgs = Msg.createRandomMsgs(num);
        for (Msg msg : msgs) {
            int id = dao.insert(msg);
            assertThat(id, greaterThan(0));
            msg.setId(id);
        }
        check(msgs, dao);
        for (Msg msg : msgs) {
            msg.setContent(Randoms.randomString(20));
        }
        dao.batchUpdate(msgs);
        check(msgs, dao);
    }

    @Test
    public void testOnePartition() {
        MsgDao dao = mango.create(MsgDao.class);
        int num = 10;
        int uid = 100;
        List<Msg> msgs = new ArrayList<Msg>();
        for (int i = 0; i < num; i++) {
            Msg msg = new Msg();
            msg.setUid(uid);
            msg.setContent(Randoms.randomString(20));
            msgs.add(msg);
            int id = dao.insert(msg);
            msg.setId(id);
        }
        check(msgs, dao);
        for (Msg msg : msgs) {
            msg.setContent(Randoms.randomString(20));
        }
        dao.batchUpdate(msgs);
        check(msgs, dao);
    }

    private void check(List<Msg> msgs, MsgDao dao) {
        List<Msg> dbMsgs = new ArrayList<Msg>();
        Multiset<Integer> ms = HashMultiset.create();
        for (Msg msg : msgs) {
            ms.add(msg.getUid());
        }
        for (Multiset.Entry<Integer> entry : ms.entrySet()) {
            dbMsgs.addAll(dao.getMsgs(entry.getElement()));
        }
        assertThat(dbMsgs, hasSize(msgs.size()));
        assertThat(dbMsgs, containsInAnyOrder(msgs.toArray()));
    }


    @DB(table = "msg", dataSourceRouter = DataSourceRouterImpl.class)
    interface MsgDao {

        @ReturnGeneratedId
        @SQL("insert into #table(uid, content) values(:1.uid, :1.content)")
        int insert(@ShardBy("uid") Msg msg);

        @SQL("update #table set content=:1.content where id=:1.id and uid=:1.uid")
        public int[] batchUpdate(@ShardBy("uid") List<Msg> msgs);

        @SQL("select id, uid, content from #table where uid=:1")
        public List<Msg> getMsgs(@ShardBy int uid);

    }

    public static class DataSourceRouterImpl implements DataSourceRouter {
        @Override
        public String getDataSourceName(Object shardParam, int type) {
            Integer uid = (Integer) shardParam;
            int tail = uid % 10;
            if (tail >= 0 && tail <= 2) {
                return dsns[0];
            } else if (tail >=3 && tail <= 5) {
                return dsns[1];
            } else {
                return dsns[2];
            }
        }
    }

}
