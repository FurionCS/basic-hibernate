package org.cs.basic.dao;


import java.util.List;
import java.util.Map;

import org.cs.basic.model.Pager;
import org.cs.basic.model.User;
import org.cs.basic.model.UserDto;
import org.springframework.stereotype.Repository;

@Repository("userDao")
public class UserDao extends BaseDao<User> implements IUserDao {

	@Override
	public List<User> listUserBySql(String string, Object[] objects,
			Map<String, Object> alias, Class<User> class1, boolean b) {
		return super.listBySql(string, objects, alias, class1, b);
	}

	@Override
	public List<User> listUserBySql(String string, Object[] objects,
			Class<User> class1, boolean b) {
		return super.listBySql(string, objects, class1, b);
	}

	@Override
	public Pager<User> findUserBySql(String string, Object[] objects,
			Class<User> class1, boolean b) {
		return super.findBySql(string, objects, class1, b);
	}

	@Override
	public Pager<User> findUserBySql(String string, Object[] objects,
			Map<String, Object> alias, Class<User> class1, boolean b) {
		return super.findBySql(string, objects, alias, class1, b);
	}

	@Override
	public UserDto sqlObjectBY(int id) {
		//测试查询对象sql
		String sql="select username as 'name' from t_user where id=?";
		return (UserDto) this.sqlObject(sql,new Object[]{id}, null, UserDto.class, false);
	}

	@Override
	public int getCountUser(int idstart,int idend) {
		String sql="select count(*) from t_user where id>? and id<?";
		return this.getCountSql(sql, new Object[]{idstart,idend}, null);
	}



}
