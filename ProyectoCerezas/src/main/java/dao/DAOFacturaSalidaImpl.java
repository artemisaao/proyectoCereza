package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import com.mysql.jdbc.Statement;

import modelos.FacturaSalida;

public class DAOFacturaSalidaImpl implements DAOFacturaSalida{
	class FacturaSalidaRowMapper implements RowMapper<FacturaSalida> {

		public FacturaSalida mapRow(ResultSet rs, int numRow) throws SQLException {
			FacturaSalida fs = new FacturaSalida(
					rs.getInt("n_factura"),
					new java.util.Date(rs.getDate("fecha").getTime()), 
					rs.getInt("iva"),
					rs.getDouble("precio_neto"),
					rs.getBoolean("anulacion"));

			return fs;
		}

	}

	/**
	 * Recupero la conexi�n con la base de datos.
	 */
	private DataSource dataSource;

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	/**
	 * Funci�n para crear un objeto FacturaSalida, que devuelve el nFactura del objeto creado.
	 * @param fs -- Objeto FacturaSalida
	 * @return nFactura
	 */
	public boolean create(final FacturaSalida fs) {
		
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);

		final String sql = "insert into factura_s (fecha, iva, precio_neto) values (?,?,?)";

		GeneratedKeyHolder kh = new GeneratedKeyHolder();
		final java.sql.Date d = new java.sql.Date(fs.getFecha().getTime());
		int n=jdbc.update(new PreparedStatementCreator() {

			public java.sql.PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement statement = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
				statement.setDate(1, d);
				statement.setInt(2, fs.getIva());
				statement.setDouble(3, fs.getPrecioNeto());
				return statement;
			}
		}, kh);
		
		
		fs.setnFactura(kh.getKey().intValue());
		return n>0;
	}

	public FacturaSalida read(int nFactura) {
		FacturaSalida fs = null;
		
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		String sql="select * from factura_s where n_factura=?";
		
		try{
			fs=jdbc.queryForObject(sql,new Object[]{nFactura},new FacturaSalidaRowMapper());
		}
		catch(IncorrectResultSizeDataAccessException ics){
			System.out.println("Read FacturaSalida -- Data access exception thrown when a result was not of the expected size, for example when expecting a single row but getting 0 or more than 1 rows.");
		}
		catch(DataAccessException dae){
			dae.printStackTrace();
			System.out.println("Read FacturaSalida -- Error acceso de datos");
		} 
		
		return fs;
	}

	/**
	 * Funci�n que modifica el objeto FacturaSalida siempre que no est� anulada. 
	 * @param fs -- Se introduce un FacturaSalida
	 * @return r -- Devuelve un boolean que determina si la funci�n se ha ejecutado correctamente o no.
	 */
	public boolean update(FacturaSalida fs) {
		boolean r=false;
		
		String sql="update factura_s set "
					+ "fecha=?, "	
					+ "iva=?, "
					+ "precio_neto=? "
				+ "where n_factura=? and anulacion=0";
		
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		
		try{
			int n=jdbc.update(
					sql,
					new Object[]{
							new java.sql.Date(fs.getFecha().getTime()),
							fs.getIva(),
							fs.getPrecioNeto(),
							fs.getnFactura()});
			r=n>0;
		}
		catch(DataAccessException dae){
			dae.printStackTrace();
			System.out.println("Update FacturaSalida -- Error acceso de datos");
			
		}
		
		return r;
	}
	
	/**
	 * Creamos una funci�n que duelve una lista con todos las facturas de salida
	 * no anuladas.
	 * @return lista
	 */
	public List<FacturaSalida> listar() {
		List<FacturaSalida> lista = null;
		
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		String sql="select * from factura_s where anulacion=0 order by fecha desc";
		lista=jdbc.query(sql,new FacturaSalidaRowMapper());
		return lista;
	}
	
	/**
	 * Funci�n sobrecargada que devuelve una lista con todos las facturas de salida
	 * anuladas y no anuladas.
	 * Sql by laura y marco A TOPE CON ELLO CHICOS, OS QUEREMOS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * @param cifNif
	 * @return lista
	 */
	public List<FacturaSalida> listar(String cifNif) {
		List<FacturaSalida> lista;
		
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		String sql="SELECT factura_s.iva, "
				+ "factura_s.precio_neto, "
				+ "factura_s.n_factura,"
				+ "factura_s.fecha,"
				+ "factura_s.anulacion "
				+ "from factura_s "
					+ "join albaranes_salida on(factura_s.n_factura=albaranes_salida.n_factura)"
					+ "join clientes on (clientes.n_cliente=albaranes_salida.n_cliente)"
					+ " join personas on (clientes.id_persona=personas.id_persona)"
				+ " WHERE personas.cif_nif =?;";
		lista=jdbc.query(sql,new Object[]{cifNif}, new FacturaSalidaRowMapper());
		return lista;
	}
	/**
	 * Recupera la lista de facturas de salida de una determinada fecha
	 * @param fecha
	 * @return lista
	 */
	
	/*public List<FacturaSalida> listar(Date fecha) {
		
		List<FacturaSalida> lista;
		
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		String sql="select * from factura_s where fecha=?";
		java.sql.Date d=new java.sql.Date(fecha.getTime());
		lista=jdbc.query(sql,new Object[]{d},new FacturaSalidaRowMapper());
		return lista;
	}*/
	/**
	 * Funci�n para acotar la fecha de busqueda
	 * anuladas y no anuladas
	 * @param fechaInicio
	 * @param fechaFinal
	 * @return lista
	 */
	
	public List<FacturaSalida> buscarFecha (Date fechaInicio, Date fechaFinal){
		
		List<FacturaSalida> lista;
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		String sql="select * from factura_s where fecha BETWEEN ? AND ? order by fecha desc;";
		java.sql.Date fi=new java.sql.Date(fechaInicio.getTime());
		java.sql.Date ff=new java.sql.Date(fechaFinal.getTime());
		lista=jdbc.query(sql,new Object[]{fi,ff},new FacturaSalidaRowMapper());
		return lista;
	}
	
	/**
	 * Metodo anularFactrura, recibe una factura y pone anulacion a true.
	 * @param FactruraEntrada fe
	 * @return true o false
	 */
	
	public boolean anularFactura(FacturaSalida fs){
		boolean r=false;
		
		String sql="update factura_s set "
					+ "anulacion=1 "
				+ "where n_factura=?";
		
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		
		try{
			int n=jdbc.update(
					sql,
					new Object[]{fs.getnFactura()});
							
			r=n>0;
		}
		catch(DataAccessException dae){
			dae.printStackTrace();
			System.out.println("Baja - Error acceso de datos");
		}
		
		return r;
				
	}
	
	/**
	 * Metodo borrar factura, Borra una factura y pone a null nFactura en los albaranes que tuviera esa factura.
	 * OJO: Se usa s�lo para los JUnit. Las facturas no se pueden borrar, solo modificar el estado
	 */
	
	public boolean delete(int nFactura) {
		
		String sql="delete from factura_s where n_factura=?" ;
		
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		
		int n=jdbc.update(sql,new Object[]{nFactura});

		return n>0;
	}
	
	public double calcularPrecioFactura(int nFactura){
		double precio = 0;
		
		String sql="select sum(lineas_albaranes_s.precio_caja*lineas_albaranes_s.numero_cajas) "
				+ "from lineas_albaranes_s join albaranes_salida "
				+ "on (lineas_albaranes_s.n_albaran=albaranes_salida.n_albaran) "
				+ "where albaranes_salida.n_factura=?";
		JdbcTemplate jdbc=new JdbcTemplate(dataSource);
		
		try{
			precio=jdbc.queryForObject(sql,new Object[]{nFactura},Double.class);
		}
		catch(IncorrectResultSizeDataAccessException ics){
			System.out.println("CalcularPrecioFactura FacturaSalida -- Data access exception thrown when a result was not of the expected size, for example when expecting a single row but getting 0 or more than 1 rows.");
		}
		catch(DataAccessException dae){
			dae.printStackTrace();
			System.out.println("CalcularPrecioFactura FacturaSalida -- Error acceso de datos");
		} 
		return precio;
	}
	
}
