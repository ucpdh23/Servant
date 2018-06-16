This project has been installed into a raspberry pi, where you have details about the deployment and execution scripts:

- requirements
  - install java (currently running with java 1.8.0_111)
  



-Installation steps:
  1.- create the folder /opt/servant
    - copy the deploy.sh script
    - copy and fill the app.json file (from the /srv/main/config folder)
    - in order to use google calendar as a scheduler, generate the client_secrent.json file from the google authorizations page
    
  2.- copy the vertx script into the /etc/init.d
    - configure the sentry license
    
  3.- run the deploy.sh script
    This script deploy and upgrade the servant application: First it shutdown the current service (is running),
     next downloads and compile a new version, finally replaces the artifacts with the new one and runs it.
      

