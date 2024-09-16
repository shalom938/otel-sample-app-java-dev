package org.springframework.samples.petclinic.owner;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RestController
public class TestJdbcController {

	@Autowired
	public ApplicationContext applicationContext;

	@GetMapping("/test/jdbc")
	public String test() throws SQLException {

		DataSource dataSource = (DataSource) applicationContext.getBeansOfType(DataSource.class).get("dataSource");
		Connection connection = dataSource.getConnection();


		PreparedStatement updateStatement = connection.prepareStatement("update owners set first_name=? where id=?");
		updateStatement.setInt(2,1);
		updateStatement.setString(1,"Shay");
		updateStatement.addBatch();
		updateStatement.setInt(2,2);
		updateStatement.setString(1,"Roni");
		updateStatement.addBatch();
		updateStatement.executeBatch();


		PreparedStatement queryStatement = connection.prepareStatement("select * from owners where id=?");
		queryStatement.setInt(1,1);
		ResultSet resultSet = queryStatement.executeQuery();
		while (resultSet.next()){
			System.out.println(resultSet.getString(2));
		}

		return "OK";
	}

}
