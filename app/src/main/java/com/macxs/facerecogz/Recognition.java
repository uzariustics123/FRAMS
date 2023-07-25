package com.macxs.facerecogz;

public interface Recognition {


    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    class Employee {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private final String id;
        /**
         * Display name for the recognition.
         */
        private final String name;
        private String firstname;
        private String lastname;
        private String companyName;
        private String branchname;


        private final Float distance;
        private Object embeddings;

        public Employee(
                final String id, final String name, final Float distance) {
            this.id = id;
            this.name = name;
            this.distance = distance;
            this.embeddings = null;

        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public void setlastname(String lastname) {
            this.lastname = lastname;
        }

        public void setBranchname(String branchname) {
            this.branchname = branchname;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getlastname() {
            return lastname;
        }

        public String getBranchname() {
            return branchname;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setFaceEmbeddings(Object embeddings) {
            this.embeddings = embeddings;
        }

        public Object getEmbeddings() {
            return this.embeddings;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (name != null) {
                resultString += firstname + " ";
            }

            if (firstname != null) {
                resultString += lastname + " ";
            }

            if (distance != null) {
                resultString += String.format("(%.1f%%) ", distance * 100.0f);
            }

            return resultString.trim();
        }

    }


}
