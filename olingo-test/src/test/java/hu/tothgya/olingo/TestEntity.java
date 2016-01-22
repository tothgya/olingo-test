package hu.tothgya.olingo;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="TEST_ENTITY", schema="TEST")
public class TestEntity {
	@Id
	@GeneratedValue
	private int id;
	
	@Column(name="DATA")
	private String data;
	
	@ManyToOne(optional = true)
	private TestEntity parent;
	
	@OneToMany(mappedBy="parent")
	private List<TestEntity> children;
	
	public void setChildren(List<TestEntity> children) {
		this.children = children;
	}
	public List<TestEntity> getChildren() {
		return children;
	}
	public void setParent(TestEntity parent) {
		this.parent = parent;
	}
	public TestEntity getParent() {
		return parent;
	}
	public int getId() {
		return id;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getData() {
		return data;
	}
	
}