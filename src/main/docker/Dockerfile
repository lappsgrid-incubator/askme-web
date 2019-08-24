FROM openjdk:9-jdk
RUN useradd -ms /bin/bash askme
RUN mkdir /usr/local/eager && chown askme /usr/local/eager
RUN mkdir /var/log/eager && chown askme /var/log/eager
COPY start.sh /home/askme/start.sh
RUN chmod +x /home/askme/start.sh
COPY *.jar /home/askme
USER askme
WORKDIR /home/askme
CMD ["/home/askme/start.sh"]
