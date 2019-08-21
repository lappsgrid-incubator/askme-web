NAME=web

include ../master.mk

web:
	docker run -d -p 8080:8080 --name $(NAME) $(IMAGE)

update:
	echo "Not implemented."
	#curl -X POST http://129.114.17.83:9000/api/webhooks/96a05d8c-978b-40d9-9c6c-bc9856318c35

