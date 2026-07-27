package zmaster587.libVulpes.util;

public class Vector3F<E> {
	public E x,y,z;
	
	
	public Vector3F(E x, E y,E z){
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public String toString() {
		return "Vector3F{" +
				"x=" + x +
				", y=" + y +
				", z=" + z +
				'}';
	}
}
