/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.model;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;
import org.traccar.helper.AdvancedConnection;
import org.traccar.helper.DriverDelegate;
import org.traccar.helper.NamedParameterStatement;

/**
 * Database abstraction class
 */
public class DatabaseDataManager implements DataManager {

    public DatabaseDataManager(Properties properties) throws Exception {
        initDatabase(properties);
    }

    /**
     * Database statements
     */
    private NamedParameterStatement queryGetDevices;
    private NamedParameterStatement queryAddPosition;
    private NamedParameterStatement queryUpdateLatestPosition;
    private NamedParameterStatement queryGetComandos;
    private NamedParameterStatement queryDeleteComando;

    /**
     * Initialize database
     */
    private void initDatabase(Properties properties) throws Exception {

        // Load driver
        String driver = properties.getProperty("database.driver");
        if (driver != null) {
            String driverFile = properties.getProperty("database.driverFile");

            if (driverFile != null) {
                URL url = new URL("jar:file:" + new File(driverFile).getAbsolutePath() + "!/");
                URLClassLoader cl = new URLClassLoader(new URL[] { url });
                Driver d = (Driver) Class.forName(driver, true, cl).newInstance();
                DriverManager.registerDriver(new DriverDelegate(d));
            } else {
                Class.forName(driver);
            }
        }

        // Refresh delay
        String refreshDelay = properties.getProperty("database.refreshDelay");
        if (refreshDelay != null) {
            devicesRefreshDelay = Long.valueOf(refreshDelay) * 1000;
        } else {
            devicesRefreshDelay = new Long(300) * 1000; // Magic number
        }

        // Connect database
        String url = properties.getProperty("database.url");
        String user = properties.getProperty("database.user");
        String password = properties.getProperty("database.password");
        AdvancedConnection connection = new AdvancedConnection(url, user, password);

        // Load statements from configuration
        String query;

        query = properties.getProperty("database.selectDevice");
        if (query != null) {
            queryGetDevices = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.insertPosition");
        if (query != null) {
            queryAddPosition = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.updateLatestPosition");
        if (query != null) {
            queryUpdateLatestPosition = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.selectComando");
        if (query != null) {
            queryGetComandos = new NamedParameterStatement(connection, query);
        }

        query = properties.getProperty("database.deleteComando");
        if (query != null) {
            queryDeleteComando = new NamedParameterStatement(connection, query);
        }
    }

    @Override
    public synchronized List<Device> getDevices() throws SQLException {

        List<Device> deviceList = new LinkedList<Device>();

        if (queryGetDevices != null) {
            queryGetDevices.prepare();
            ResultSet result = queryGetDevices.executeQuery();
            while (result.next()) {
                Device device = new Device();
                device.setId(result.getLong("id"));
                device.setImei(result.getString("imei"));
                deviceList.add(device);
            }
        }

        return deviceList;
    }

    /**
     * Devices cache
     */
    private Map<String, Device> devices;
    private Calendar devicesLastUpdate;
    private Long devicesRefreshDelay;

    @Override
    public Device getDeviceByImei(String imei) throws SQLException {

        if ((devices == null) || (Calendar.getInstance().getTimeInMillis() - devicesLastUpdate.getTimeInMillis() > devicesRefreshDelay)) {
            List<Device> list = getDevices();
            devices = new HashMap<String, Device>();
            for (Device device: list) {
                devices.put(device.getImei(), device);
            }
            devicesLastUpdate = Calendar.getInstance();
        }

        return devices.get(imei);
    }

    @Override
    public synchronized Long addPosition(Position position) throws SQLException {

        if (queryAddPosition != null) {
            queryAddPosition.prepare(Statement.RETURN_GENERATED_KEYS);

            queryAddPosition.setLong("id", position.getId());
            queryAddPosition.setLong("device_id", position.getDeviceId());
            queryAddPosition.setTimestamp("time", position.getTime());
            queryAddPosition.setBoolean("valid", position.getValid());
            queryAddPosition.setDouble("altitude", position.getAltitude());
            queryAddPosition.setDouble("latitude", position.getLatitude());
            queryAddPosition.setDouble("longitude", position.getLongitude());
            queryAddPosition.setDouble("speed", position.getSpeed());
            queryAddPosition.setDouble("course", position.getCourse());
            queryAddPosition.setDouble("power", position.getPower());
            queryAddPosition.setString("address", position.getAddress());
            queryAddPosition.setString("extended_info", position.getExtendedInfo());

            queryAddPosition.executeUpdate();

            ResultSet result = queryAddPosition.getGeneratedKeys();
            if (result != null && result.next()) {
                return result.getLong(1);
            }
        }

        return null;
    }

    @Override
    public void updateLatestPosition(Long deviceId, Long positionId) throws SQLException {
        
        if (queryUpdateLatestPosition != null) {
            queryUpdateLatestPosition.prepare();

            queryUpdateLatestPosition.setLong("device_id", deviceId);
            queryUpdateLatestPosition.setLong("id", positionId);

            queryUpdateLatestPosition.executeUpdate();
        }
    }
    @Override
    public synchronized List<Comando> getComandos() throws SQLException {

        List<Comando> comandoList = new LinkedList<Comando>();

        if (queryGetComandos != null) {
            queryGetComandos.prepare();
            ResultSet result = queryGetComandos.executeQuery();
            while (result.next()) {
                Comando comando = new Comando();
                comando.setImei(result.getString("imei"));
                comando.setCommand(result.getString("command"));
                comandoList.add(comando);
                //Log.warning("getComandos - " + comando.getCommand());
            }
        }

        return comandoList;
    }
    private Map<String, Comando> comandos;
    @Override
    public Comando getComandoByImei(String imei) throws SQLException {
            List<Comando> list = getComandos();
            comandos = new HashMap<String, Comando>();
            for (Comando comando : list) {
                comandos.put(comando.getImei(), comando);
            }
            devicesLastUpdate = Calendar.getInstance();
        return comandos.get(imei);
    }
        @Override
    public void deleteComando(String imei, String command) throws SQLException {

        if (queryDeleteComando != null) {
            queryDeleteComando.prepare();

            queryDeleteComando.setString("command", command);
            queryDeleteComando.setString("imei", imei);

            queryDeleteComando.executeUpdate();
        }
    }
}
