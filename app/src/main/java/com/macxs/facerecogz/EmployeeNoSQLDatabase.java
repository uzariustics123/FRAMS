package com.macxs.facerecogz;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class EmployeeNoSQLDatabase {
    SharedPreferences sharedPrefs;
    Gson gson = new Gson();
    Context context;

    public EmployeeNoSQLDatabase(Context context) {
        sharedPrefs = context.getSharedPreferences("EmployeesJSONDB", Context.MODE_PRIVATE);
        this.context = context;
    }

    public HashMap<String, Recognition.Employee> getRegisteredEmployees() {
        String jsondata = sharedPrefs.getString("employees", "");
        TypeToken<HashMap<String,Recognition.Employee>> token = new TypeToken<HashMap<String,Recognition.Employee>>() {};
        TypeToken<ArrayList<HashMap<String,Recognition.Employee>>> token2 = new TypeToken<ArrayList<HashMap<String,Recognition.Employee>>>() {};
        TypeToken<ArrayList<HashMap<String,Object>>> token3 = new TypeToken<ArrayList<HashMap<String,Object>>>() {};
        if (!jsondata.isEmpty()) {
            return gson.fromJson(jsondata, token.getType());
        }
        return new HashMap<String, Recognition.Employee>();
    }
    public ArrayList<HashMap<String,Object>> getRegisteredEmplyeesList(){
        String jsondata = sharedPrefs.getString("employees", "");
        TypeToken<ArrayList<HashMap<String,Object>>> token = new TypeToken<ArrayList<HashMap<String,Object>>>() {};
        ArrayList<HashMap<String, Object>> retreivedList = gson.fromJson(jsondata, token.getType());
        ArrayList<HashMap<String, Object>> formattedList = new ArrayList<>();
        if (retreivedList != null) {
            for (HashMap<String, Object> employee: retreivedList) {
                int OUTPUT_SIZE=192;
                float[][] output=new float[1][OUTPUT_SIZE];
                ArrayList arrayList= (ArrayList) employee.get("face_data");
                arrayList = (ArrayList) arrayList.get(0);
                for (int counter = 0; counter < arrayList.size(); counter++) {
                    output[0][counter]= ((Double) arrayList.get(counter)).floatValue();
                }
                employee.put("face_data", output);
                System.out.println("Entry output " +Arrays.deepToString(output) );
                formattedList.add(employee);
            }
            return formattedList;
        }
        return new ArrayList<HashMap<String, Object>>();
    }

    public void saveNewEmployeeData(HashMap<String, Object> employeeData) {
        ArrayList<HashMap<String, Object>> registereds = (ArrayList<HashMap<String, Object>>) getRegisteredEmplyeesList();
        registereds.add(employeeData);
        saveRegisteredEmployees(registereds);
    }
    public String getRegisteredJSONstr(){
        return sharedPrefs.getString("employees", "");
    }

    public void saveRegisteredEmployees(ArrayList registeredEmployees) {
        String jsondata = gson.toJson(registeredEmployees);
        sharedPrefs.edit().putString("employees", jsondata).commit();
    }
}
