package org.cn;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

public class Appointment {
	public static final String TABLE_NAME = "appointments";
	private int id;
	private UUID uuid;
	private String header;
	private String startAt;
	private String durationInMinutes;
	private String comment;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getStartAt() {
		return startAt;
	}

	public void setStartAt(String startAt) {
		this.startAt = startAt;
	}

	public String getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(String durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Appointment() {
	}

	public Appointment(String header, String startAt, String durationInMinutes,
			String comment) {
		this.header = header;
		this.startAt = startAt;
		this.durationInMinutes = durationInMinutes;
		this.comment = comment;
	}
	
	@SuppressWarnings("unchecked")
	public static void synchronize(XmlRpcClient remoteServer) throws XmlRpcException,
			NoSuchAlgorithmException, SQLException {
		Map<String, Object[]> delta = (Map<String, Object[]>) remoteServer
				.execute("appointment.get_delta",
						new Object[] { allIdChecksumPairs() });
	
		Object[] coll = (Object[]) delta.get("destroy");
		// perform destroy
		for (Object obj : coll) {
			Appointment.destroy(UUID.fromString(((Map<String, String>) obj).get("uuid")));
		}

		// perform update
		coll = (Object[]) delta.get("update");
		for (Object obj : coll) {
			Appointment.findAndModifyWith((Map<String, String>) obj).save();
		}

		// perform create
		coll = (Object[]) delta.get("create");
		for (Object obj : coll) {
			Appointment.newFrom((Map<String, String>) obj).create();
		}
	}
	
	/**
	 * based on uuid of given xml rpc appintment
	 * find relevant appointment and set its other fields to the fields
	 * from xml rcp appointment
	 * @param xmlRpcAppointment
	 * @return
	 * @throws SQLException
	 */
	public static Appointment findAndModifyWith(Map<String, String> xmlRpcAppointment) throws SQLException {
		Appointment appointment = Appointment.find(UUID.fromString(xmlRpcAppointment
				.get("uuid")));
		
		appointment.setHeader(xmlRpcAppointment.get("header"));
		appointment.setStartAt(xmlRpcAppointment.get("start_at"));
		appointment.setDurationInMinutes(xmlRpcAppointment
				.get("duration_in_minutes"));
		appointment.setComment(xmlRpcAppointment.get("comment"));
		
		return appointment;
	}
	
	/**
	 * based on given xml rpc representation of appointment
	 * init new appointment and return it
	 * @param xmlRpcAppointment
	 * @return
	 */
	public static Appointment newFrom(Map<String, String> xmlRpcAppointment) {
		Appointment appointment = new Appointment();
		
		appointment.setUuid(UUID.fromString(xmlRpcAppointment
				.get("uuid")));
		appointment.setHeader(xmlRpcAppointment.get("header"));
		appointment.setStartAt(xmlRpcAppointment.get("start_at"));
		appointment.setDurationInMinutes(xmlRpcAppointment
				.get("duration_in_minutes"));
		appointment.setComment(xmlRpcAppointment.get("comment"));
		
		return appointment;
	}

	public boolean create() throws SQLException {
		String sql = null;
		if (uuid == null) {
			setUuid(UUID.randomUUID());
		}
		sql = "INSERT INTO " + TABLE_NAME
				+ " (id, uuid, header, startAt, durationInMinutes, comment) "
				+ "VALUES (NULL, '" + uuid.toString() + "', '" + header + "', '"
				+ startAt + "', '" + durationInMinutes + "', '" + comment
				+ "');";
		Connection conn = Database.getConnection();
		Statement stmt = conn.createStatement();
		int res = stmt.executeUpdate(sql);
		stmt.close();
		conn.close();

		return res > 0;
	}

	// TODO implement
	public boolean save() throws SQLException {
		String sql = null;

		sql = "UPDATE " + TABLE_NAME + " SET " + " header='" + header
				+ "', startAt='" + startAt + "', durationInMinutes='"
				+ durationInMinutes + "', comment='" + comment
				+ "' WHERE uuid='" + uuid.toString() + "';";

		Connection conn = Database.getConnection();
		Statement stmt = conn.createStatement();
		int res = stmt.executeUpdate(sql);
		stmt.close();
		conn.close();

		return res > 0;
	}

	public static Appointment find(UUID uuid) throws SQLException {
		Appointment appointment = null;

		Connection conn = Database.getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM " + TABLE_NAME
				+ " WHERE uuid='" + uuid.toString() + "' LIMIT 1;");
		if (rs.next()) {
			appointment = new Appointment(rs.getString("header"),
					rs.getString("startAt"), rs.getString("durationInMinutes"),
					rs.getString("comment"));
			appointment.setUuid(UUID.fromString(rs.getString("uuid")));
		}
		rs.close();
		stmt.close();
		conn.close();

		return appointment;
	}
	
	public void reload() throws SQLException {
		Appointment newAppointment = Appointment.findById(this.getId());
		this.header = newAppointment.getHeader();
		this.durationInMinutes = newAppointment.getDurationInMinutes();
		this.startAt = newAppointment.getStartAt();
		this.comment = newAppointment.getComment();
	}
	
	public static Appointment findById(int id) throws SQLException {
		Appointment appointment = null;
		
		Connection conn = Database.getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM " + TABLE_NAME
				+ " WHERE id='" + id + "' LIMIT 1;");
		if (rs.next()) {
			appointment = new Appointment(rs.getString("header"),
					rs.getString("startAt"), rs.getString("durationInMinutes"),
					rs.getString("comment"));
			appointment.setId(rs.getInt("id"));
			appointment.setUuid(UUID.fromString(rs.getString("uuid")));
		}
		rs.close();
		stmt.close();
		conn.close();

		return appointment;
	}

	/**
	 * 
	 * @return
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 */
	public static Map<String, String> allIdChecksumPairs() throws SQLException,
			NoSuchAlgorithmException {
		List<Appointment> allAppointments = all();
		Map<String, String> pairs = new HashMap<String, String>();

		for (Appointment appointment : allAppointments) {
			pairs.put(appointment.uuid.toString(), appointment.getChecksum());
		}

		return pairs;
	}

	public static List<Appointment> all() throws SQLException {
		List<Appointment> ret = new ArrayList<Appointment>();
		Appointment tmpAppointment = null;
		Connection conn = Database.getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM " + TABLE_NAME + ";");
		while (rs.next()) {
			tmpAppointment = new Appointment(rs.getString("header"),
					rs.getString("startAt"), rs.getString("durationInMinutes"),
					rs.getString("comment"));
			tmpAppointment.setUuid(UUID.fromString(rs.getString("uuid")));
			tmpAppointment.setId(rs.getInt("id"));
			ret.add(tmpAppointment);
		}
		rs.close();
		stmt.close();
		conn.close();

		return ret;
	}

	public Map<String, String> xmlRpcCompatible() {
		Map<String, String> ret = new HashMap<String, String>();
		ret.put("uuid", uuid.toString());
		ret.put("header", header == null ? "" : header);
		ret.put("start_at", startAt == null ? "" : startAt);
		ret.put("duration_in_minutes", durationInMinutes == null ? ""
				: durationInMinutes);
		ret.put("comment", comment == null ? "" : comment);
		return ret;
	}

	public static boolean destroy(UUID uuid) throws SQLException {
		Connection conn = Database.getConnection();
		Statement stmt = conn.createStatement();
		int res = stmt.executeUpdate("DELETE FROM " + TABLE_NAME
				+ " WHERE uuid='" + uuid.toString() + "';");
		stmt.close();
		conn.close();
		return res > 0;
	}
	
	public boolean destroy() throws SQLException {
		Connection conn = Database.getConnection();
		Statement stmt = conn.createStatement();
		int res = stmt.executeUpdate("DELETE FROM " + TABLE_NAME
				+ " WHERE uuid='" + uuid + "';");
		stmt.close();
		conn.close();
		return res > 0;
	}

	private String getChecksum() throws NoSuchAlgorithmException {
		String input = header + startAt + durationInMinutes + comment;
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] result = mDigest.digest(input.getBytes());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16)
					.substring(1));
		}

		return sb.toString();
	}
}
