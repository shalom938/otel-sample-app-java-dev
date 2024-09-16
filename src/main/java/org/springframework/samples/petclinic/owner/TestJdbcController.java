package org.springframework.samples.petclinic.owner;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.samples.petclinic.mypkg.MyClass;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.*;

@RestController
public class TestJdbcController {

	@Autowired
	public ApplicationContext applicationContext;

	@GetMapping("/test/jdbc")
	public String test() throws SQLException {

		//just call this method to check that extended observability works
		new MyClass().myMethod();


		DataSource dataSource = (DataSource) applicationContext.getBeansOfType(DataSource.class).get("dataSource");
		Connection connection = dataSource.getConnection();


		{
			PreparedStatement updateStatement = connection.prepareStatement("update owners set first_name=? where id=?");
			updateStatement.setInt(2, 1);
			updateStatement.setString(1, "Foo");
			updateStatement.addBatch();
			updateStatement.setInt(2, 2);
			updateStatement.setString(1, "Bar");
			updateStatement.addBatch();
			updateStatement.setInt(2, 3);
			updateStatement.setString(1, "Bar2");
			updateStatement.addBatch();
			updateStatement.executeBatch();
		}

		StringBuilder result = new StringBuilder();

		{
			PreparedStatement queryStatement = connection.prepareStatement("select * from owners where id=?");
			queryStatement.setInt(1, 1);
			ResultSet resultSet = queryStatement.executeQuery();
			result.append("[");
			while (resultSet.next()) {
				result.append(resultSet.getString(2)).append(",");
			}
			result.append("] ");
			queryStatement.close();
		}

		result.append('\n');

		{
			PreparedStatement queryStatement = connection.prepareStatement("select * from owners where id=? and first_name=?");
			queryStatement.setInt(1, 1);
			queryStatement.setString(2, "Foo");
			ResultSet resultSet = queryStatement.executeQuery();
			result.append("[");
			while (resultSet.next()) {
				result.append(resultSet.getString(2)).append(",");
			}
			result.append("] ");
			queryStatement.close();
		}

		result.append('\n');

		{
			PreparedStatement queryStatement = connection.prepareStatement("select * from owners where id=? and first_name like ?");
			queryStatement.setInt(1, 2);
			queryStatement.setString(2, "%Ba%");
			ResultSet resultSet = queryStatement.executeQuery();
			result.append("[");
			while (resultSet.next()) {
				result.append(resultSet.getString(2)).append(",");
			}
			result.append("] ");
			queryStatement.close();
		}

		{
			//user can write the query like "first_name like '?%'"
			//and setString(1, "Ba");
			//in that case our query with parameters will look like "first_name like ''Ba%''"  which is not good

			PreparedStatement queryStatement = connection.prepareStatement("select * from owners where first_name like ?");
			queryStatement.setString(1, "Ba%");
			ResultSet resultSet = queryStatement.executeQuery();
			result.append("[");
			while (resultSet.next()) {
				result.append(resultSet.getString(2)).append(",");
			}
			result.append("] ");
			queryStatement.close();
		}

		result.append('\n');

		{
			//test no params
			PreparedStatement queryStatement = connection.prepareStatement("select * from owners where id=2 and first_name like '%Ba%'");
			ResultSet resultSet = queryStatement.executeQuery();
			result.append("[");
			while (resultSet.next()) {
				result.append(resultSet.getString(2)).append(",");
			}
			result.append("] ");
			queryStatement.close();
		}

		result.append('\n');

		{
			//regular statement
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("select * from owners where first_name like '%F%'");
			result.append("[");
			while (resultSet.next()) {
				result.append(resultSet.getString(2)).append(",");
			}
			result.append("] ");
			statement.close();
		}




		System.out.println("TestJdbcController result = " + result);

		return result.toString();
	}

}
