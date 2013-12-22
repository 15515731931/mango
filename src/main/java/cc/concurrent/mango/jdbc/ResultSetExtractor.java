package cc.concurrent.mango.jdbc;


import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author ash
 */
public interface ResultSetExtractor<T> {


    T extractData(ResultSet rs) throws SQLException;


}