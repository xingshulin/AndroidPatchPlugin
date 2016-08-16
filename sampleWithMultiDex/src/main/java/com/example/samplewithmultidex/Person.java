package com.example.samplewithmultidex;

import android.os.Parcel;
import android.os.Parcelable;

public class Person implements Parcelable {
    private String firstName;
    private String lastName;
    private int age;

    public Person(String firstName, String lastName, int age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

    protected Person(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<Person> CREATOR = new Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel in) {
            return new Person(in);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeInt(age);
    }

    public void readFromParcel(Parcel in) {
        firstName = in.readString();
        lastName = in.readString();
        age = in.readInt();
    }

    public String display() {
        return String.format("%s, %s %s years", firstName, lastName, age);
    }
}
