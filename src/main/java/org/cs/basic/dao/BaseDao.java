/**
 * 
 */
package org.cs.basic.dao;

import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cs.basic.model.Pager;
import org.cs.basic.model.SystemContext;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Administrator
 *
 */
@SuppressWarnings("unchecked")
public class BaseDao<T> implements IBaseDao<T> {
	
	private SessionFactory sessionFactory;
	/**
	 * 创建一个Class的对象来获取泛型的class
	 */
	private Class<?> clz;
	
	public Class<?> getClz() {
		if(clz==null) {
			//获取泛型的Class对象
			clz = ((Class<?>)
					(((ParameterizedType)(this.getClass().getGenericSuperclass())).getActualTypeArguments()[0]));
		}
		return clz;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	@Autowired
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	protected Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	/**
	 * 保存一个对象
	 * @param t 对象
	 */
	@Override
	public T add(T t) {
		getSession().save(t);
		return t;
	}

	/**
	 * 更新一个对象
	 * @param t 对象
	 */
	@Override
	public void update(T t) {
		getSession().update(t);
	}

	/**
	 * 删除一个对象
	 * @param id
	 */
	@Override
	public void delete(int id) {
		getSession().delete(this.load(id));
	}

	/**
	 * 加载一个对象
	 * @param id
	 */
	@Override
	public T load(int id) {
		return (T)getSession().load(getClz(), id);
	}

	/**
	 * 获得列表
	 * @param hql   
	 * @param args  一组问号的值
	 * @return
	 */
	public List<T> list(String hql, Object[] args) {
		return this.list(hql, args, null);
	}

	/**
	 * 获得列表对象
	 * @param hql
	 * @param arg  一个问号的值
	 * @return
	 */
	public List<T> list(String hql, Object arg) {
		return this.list(hql, new Object[]{arg});
	}

	/**
	 * 获得列表
	 * @param hql
	 * @return
	 */
	public List<T> list(String hql) {
		return this.list(hql,null);
	}
	
	private String initSort(String hql) {
		String order = SystemContext.getOrder();
		String sort = SystemContext.getSort();
		if(sort!=null&&!"".equals(sort.trim())) {
			hql+=" order by "+sort;
			if(!"desc".equals(order)) hql+=" asc";
			else hql+=" desc";
		}
		return hql;
	}
	
	@SuppressWarnings("rawtypes")
	private void setAliasParameter(Query query,Map<String,Object> alias) {
		if(alias!=null) {
			Set<String> keys = alias.keySet();
			for(String key:keys) {
				Object val = alias.get(key);
				if(val instanceof Collection) {
					//查询条件是列表
					query.setParameterList(key, (Collection)val);
				} else {
					query.setParameter(key, val);
				}
			}
		}
	}
	
	private void setParameter(Query query,Object[] args) {
		if(args!=null&&args.length>0) {
			int index = 0;
			for(Object arg:args) {
				query.setParameter(index++, arg);
			}
		}
	}

	/**
	 * 获得列表
	 * @param hql
	 * @param args   一组问号的值
	 * @param alias   map<String,Object> 别名
	 * @return
	 */
	public List<T> list(String hql, Object[] args, Map<String, Object> alias) {
		hql = initSort(hql);
		Query query = getSession().createQuery(hql);
		setAliasParameter(query, alias);
		setParameter(query, args);
		return query.list();
	}

	/**
	 * 获得列表 通过一个别名
	 * @param hql
	 * @param alias map<String,Object> 
	 * @return
	 */
	public List<T> listByAlias(String hql, Map<String, Object> alias) {
		return this.list(hql, null, alias);
	}

	/**
	 * 分页查找
	 * @param hql
	 * @param args
	 * @return
	 */
	public Pager<T> find(String hql, Object[] args) {
		return this.find(hql, args, null);
	}

	/**
	 * 分页查找
	 * @param hql
	 * @param arg 问号值
	 * @return 
	 */
	public Pager<T> find(String hql, Object arg) {
		return this.find(hql, new Object[]{arg});
	}

	/**
	 * 分页列表
	 * @param hql
	 * @return
	 */
	public Pager<T> find(String hql) {
		return this.find(hql,null);
	}
	
	@SuppressWarnings("rawtypes")
	private void setPagers(Query query,Pager pages) {
		Integer pageSize = SystemContext.getPageSize();
		Integer pageOffset = SystemContext.getPageOffset();
		if(pageOffset==null||pageOffset<0) pageOffset = 0;
		if(pageSize==null||pageSize<0) pageSize = 15;
		pages.setOffset(pageOffset);
		pages.setSize(pageSize);
		query.setFirstResult(pageOffset).setMaxResults(pageSize);
	}
	
	private String getCountHql(String hql,boolean isHql) {
		String e = hql.substring(hql.indexOf("from"));
		String c = "select count(*) "+e;
		if(isHql)
			c.replaceAll("fetch", "");
		return c;
	}

	/**
	 * 分页列表
	 * @param hql
	 * @param args  一组问号值
	 * @param alias 别名
	 * @return
	 */
	public Pager<T> find(String hql, Object[] args, Map<String, Object> alias) {
		hql = initSort(hql);
		String cq = getCountHql(hql,true);
		Query cquery = getSession().createQuery(cq);
		Query query = getSession().createQuery(hql);
		//设置别名参数
		setAliasParameter(query, alias);
		setAliasParameter(cquery, alias);
		//设置参数
		setParameter(query, args);
		setParameter(cquery, args);
		Pager<T> pages = new Pager<T>();
		setPagers(query,pages);
		List<T> datas = query.list();
		pages.setDatas(datas);
		long total = (Long)cquery.uniqueResult();
		pages.setTotal(total);
		return pages;
	}

	/**
	 * 通过别名获得分页列表
	 * @param hql
	 * @param alias  别名
	 * @return
	 */
	public Pager<T> findByAlias(String hql, Map<String, Object> alias) {
		return this.find(hql,null, alias);
	}

	/**
	 * 获得对象
	 * @param hql
	 * @param args 一组问号值
	 * @return
	 */
	public Object queryObject(String hql, Object[] args) {
		return this.queryObject(hql, args,null);
	}

	/**
	 * 获得对象
	 * @param hql
	 * @param arg 问号
	 * @return
	 */
	public Object queryObject(String hql, Object arg) {
		return this.queryObject(hql, new Object[]{arg});
	}

	/**
	 * 获得对象
	 * @param hql
	 * @return
	 */
	public Object queryObject(String hql) {
		return this.queryObject(hql,null);
	}

	/**
	 * 更新对象通过hql
	 * @param hql
	 * @param args 一组问号值
	 */
	public void updateByHql(String hql, Object[] args) {
		Query query = getSession().createQuery(hql);
		setParameter(query, args);
		query.executeUpdate();
	}

	/**
	 * 更新对象通过hql
	 * @param hql
	 * @param arg 问号值
	 */
	public void updateByHql(String hql, Object arg) {
		this.updateByHql(hql,new Object[]{arg});
	}

	/**
	 * 更新对象通过hql
	 * @param hql
	 */
	public void updateByHql(String hql) {
		this.updateByHql(hql,null);
	}

	/**
	 * 获得列表通过sql 
	 * @param sql
	 * @param args  问号组
	 * @param clz   实体对象     结果包装成实体 
	 * @param hasEntity  是否是实体
	 * @return
	 */
	public <N extends Object>List<N> listBySql(String sql, Object[] args, Class<?> clz,
			boolean hasEntity) {
		return this.listBySql(sql, args, null, clz, hasEntity);
	}

	/**
	 * 获得列表通过sql 
	 * @param sql
	 * @param args  问号值
	 * @param clz   实体对象     结果包装成实体 
	 * @param hasEntity  是否是实体
	 * @return
	 */
	public <N extends Object>List<N> listBySql(String sql, Object arg, Class<?> clz,
			boolean hasEntity) {
		return this.listBySql(sql, new Object[]{arg}, clz, hasEntity);
	}

	/**
	 * 获得列表通过sql 
	 * @param sql
	 * @param clz  实体对象     结果包装成实体 
	 * @param hasEntity 是否是实体
	 * @return
	 */
	public <N extends Object>List<N> listBySql(String sql, Class<?> clz, boolean hasEntity) {
		return this.listBySql(sql, null, clz, hasEntity);
	}

	/**
	 * 获得列表通过sql 
	 * @param sql
	 * @param args  问号值
	 * @param alias 别名
	 * @param clz  实体对象     结果包装成实体 
	 * @param hasEntity 是否是实体
	 * @return
	 */
	public <N extends Object>List<N> listBySql(String sql, Object[] args,
			Map<String, Object> alias, Class<?> clz, boolean hasEntity) {
		sql = initSort(sql);
		SQLQuery sq = getSession().createSQLQuery(sql);
		setAliasParameter(sq, alias);
		setParameter(sq, args);
		if(hasEntity) {
			sq.addEntity(clz);
		} else 
			sq.setResultTransformer(Transformers.aliasToBean(clz));
		return sq.list();
	}

	/**
	 * 通过别名获得列表
	 * @param sql
	 * @param alias 别名
	 * @param clz   实体对象     结果包装成实体 
	 * @param hasEntity 是否是实体
	 * @return
	 */
	public <N extends Object>List<N> listByAliasSql(String sql, Map<String, Object> alias,
			Class<?> clz, boolean hasEntity) {
		return this.listBySql(sql, null, alias, clz, hasEntity);
	}

	/**
	 * 分页获得列表
	 * @param sql
	 * @param args 别名
	 * @param clz  实体对象     结果包装成实体 
	 * @param hasEntity  是否是实体
	 * @return
	 */
	public <N extends Object>Pager<N> findBySql(String sql, Object[] args, Class<?> clz,
			boolean hasEntity) {
		return this.findBySql(sql, args, null, clz, hasEntity);
	}

	/**
	 * 
	 * 分页获得列表
	 * @param sql
	 * @param args 别名组
	 * @param clz  实体对象     结果包装成实体 
	 * @param hasEntity  是否是实体
	 * @return
	 */
	public <N extends Object>Pager<N> findBySql(String sql, Object arg, Class<?> clz,
			boolean hasEntity) {
		return this.findBySql(sql, new Object[]{arg}, clz, hasEntity);
	}

	/**
	 * 分页获得列表
	 * @param sql
	 * @param clz 实体对象     结果包装成实体 
	 * @param hasEntity  是否是实体
	 * @return
	 */
	public <N extends Object>Pager<N> findBySql(String sql, Class<?> clz, boolean hasEntity) {
		return this.findBySql(sql, null, clz, hasEntity);
	}

	/**
	 * 分页获得列表
	 * @param sql
	 * @param args   问号值
	 * @param alias  别名
	 * @param clz    实体对象     结果包装成实体 
	 * @param hasEntity 是否是实体
	 * @return
	 */
	public <N extends Object>Pager<N> findBySql(String sql, Object[] args,
			Map<String, Object> alias, Class<?> clz, boolean hasEntity) {
		sql = initSort(sql);
		String cq = getCountHql(sql,false);
		SQLQuery sq = getSession().createSQLQuery(sql);
		SQLQuery cquery = getSession().createSQLQuery(cq);
		setAliasParameter(sq, alias);
		setAliasParameter(cquery, alias);
		setParameter(sq, args);
		setParameter(cquery, args);
		Pager<N> pages = new Pager<N>();
		setPagers(sq, pages);
		if(hasEntity) {
			sq.addEntity(clz);
		} else {
			sq.setResultTransformer(Transformers.aliasToBean(clz));
		}
		List<N> datas = sq.list();
		pages.setDatas(datas);
		long total = ((BigInteger)cquery.uniqueResult()).longValue();
		pages.setTotal(total);
		return pages;
	}

	/**
	 * 分页通过别名获得列表
	 * @param sql
	 * @param alias 别名
	 * @param clz   实体对象     结果包装成实体 
	 * @param hasEntity 是否是实体
	 * @return
	 */
	public <N extends Object>Pager<N> findByAliasSql(String sql, Map<String, Object> alias,
			Class<?> clz, boolean hasEntity) {
		return this.findBySql(sql, null, alias, clz, hasEntity);
	}
	/**
	 * 获得对象
	 * @param hql
	 * @param args  问号
	 * @param alias 别名
	 * @return
	 */
	public Object queryObject(String hql, Object[] args,
			Map<String, Object> alias) {
		Query query = getSession().createQuery(hql);
		setAliasParameter(query, alias);
		setParameter(query, args);
		return query.uniqueResult();
	}
	/**
	 * 通过别名获得对象
	 * @param hql
	 * @param alias 别名
	 * @return
	 */
	public Object queryObjectByAlias(String hql, Map<String, Object> alias) {
		return this.queryObject(hql,null,alias);
	}

}
