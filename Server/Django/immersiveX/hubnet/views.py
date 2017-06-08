from django.views.decorators.csrf import csrf_protect, csrf_exempt
from django.http import HttpResponse, Http404
from django.shortcuts import render
from django.conf import settings

import string
import random
import time

from django.db import connection

import csv
import json
import datetime

from .models import Event, Group, Sensor, Path, Midpoint, Participant, Record, Floor

'''
	views.py IE 2.0
	Author: Moreno Colombo
	Creation date: 28.12.2016
	Last modified: 27.01.2017
'''


'''
	UTILITIES
'''
# Check if server is running correctly
def server_state(request):
	return HttpResponse("ok")

# Get application version
def ver(request):
    return HttpResponse("<p>Immersive Experience 2.0</p><p>Moreno Colombo & SICHH </p>")

'''
	TABLES
	Views to show personalized tables
'''	
def show_table(request, type, eventId = None):
	# Minimum duration of the visit of a room (in seconds)
	minTime = 20
	response = get_table(type, eventId, minTime)
	
	if response == "Invalid table type":
		raise Http404(response)
	
	return render(request, "hubnet/tables.html", {"tableData": response})


#### ONLY FOR TESTING!!!
@csrf_exempt
def add_records(request):
	event = Event.objects.get(name = "Test_a")
	sensor = Sensor.objects.filter(event = event)
	
	# Save records in bulk
	t = datetime.datetime.now()
	records = []
	for i in range(1,260):
		p = Participant.objects.filter(event = event).order_by('?').first()
		sens = sensor.order_by('?').first()
		records.append(Record(event=event, sensor=sens, timestamp=t, participant = p))
		t = t + datetime.timedelta(seconds = 30)
		records.append(Record(event=event, sensor=sens, timestamp=t, participant = p))
		t = t + datetime.timedelta(seconds = 5)
	Record.objects.bulk_create(records)
	
	return HttpResponse("ok\n")	
	
'''
	DATA ANALYSIS
	The views for data visualization (Maybe to be moved in another file)
'''	
# View most taken paths for an event
def visualize_paths(request, eventId=0, floor=None):
	if eventId==0:
		response = {"viz":"paths"}
		response["data"] = [{"id": e.id, "name": e.name} for e in Event.objects.order_by("-startDate")]
		response["floors"] = [{"id": f.id, "e_id": f.event_id, "name": str(f)} for f in Floor.objects.all()]
		return render(request, "hubnet/choose_event.html", {"tableData": response})

	response = get_table("movements", eventId)
	return render(request, "hubnet/visualize_paths.html", {"tableData": response, "floor": floor})

# Visualize animation of people moving at the event
def visualize_movements(request, eventId=0, speed=1, floorId=None):
	if eventId==0:
		response = {"viz":"movements"}
		response["data"] = [{"id": e.id, "name": e.name} for e in Event.objects.order_by("-startDate")]
		response["floors"] = [{"id": f.id, "e_id": f.event_id, "name": str(f)} for f in Floor.objects.all()]
		return render(request, "hubnet/choose_event.html", {"tableData": response})

	event = Event.objects.get(id = eventId);
	
	bg = event.background or ""
	
	response = get_table("movements", eventId)
	response["event"] = {"start": str(event.startDate), "end": str(event.stopDate), "duration": (event.stopDate-event.startDate).total_seconds()}
	response["paths"]={}
	
	for p in Path.objects.filter(event_id = eventId):
		if floorId!= None:
			floor = Floor.objects.get(id = floorId)
			if p.startSensor.x >= floor.x and p.startSensor.x <= floor.x+floor.width and p.startSensor.y >= floor.y and p.startSensor.y <= floor.y+floor.height and p.endSensor.x >= floor.x and p.endSensor.x <= floor.x+floor.width and p.endSensor.y >= floor.y and p.endSensor.y <= floor.y+floor.height:				
				response["paths"][str(p.startSensor.sensorId)+"_"+str(p.endSensor.sensorId)] = [{"x": mp.x, "y": mp.y} for mp in Midpoint.objects.filter(path = p).order_by("order")]
				response["paths"][str(p.endSensor.sensorId)+"_"+str(p.startSensor.sensorId)] = [{"x": mp.x, "y": mp.y} for mp in Midpoint.objects.filter(path = p).order_by("-order")]
		else:
			response["paths"][str(p.startSensor.sensorId)+"_"+str(p.endSensor.sensorId)] = [{"x": mp.x, "y": mp.y} for mp in Midpoint.objects.filter(path = p).order_by("order")]
			response["paths"][str(p.endSensor.sensorId)+"_"+str(p.startSensor.sensorId)] = [{"x": mp.x, "y": mp.y} for mp in Midpoint.objects.filter(path = p).order_by("-order")]
	response["speed"] = speed
	if floorId!=None:
		f = Floor.objects.get(id = floorId)
		response["floor"] = {"x": f.x, "y": f.y, "width": f.width, "height": f.height}
	return render(request, "hubnet/visualize_movements.html", {"tableData": response, "backgroundImage": bg})

# Visualize animation of people moving at the event
def visualize_rec_movements(request, eventId=0):
	if eventId==0:
		response = {"viz":"rec_movements"}
		response["data"] = [{"id": e.id, "name": e.name} for e in Event.objects.order_by("-startDate")]
		return render(request, "hubnet/choose_event.html", {"tableData": response})

	event = Event.objects.get(id = eventId);
	
	bg = event.background or ""
	
	response = get_table("movements", eventId)
	response["event"] = {"start": str(event.startDate), "end": str(event.stopDate), "duration": (event.stopDate-event.startDate).total_seconds()}
	response["paths"]={}
	for p in Path.objects.filter(event_id = eventId):
		response["paths"][str(p.startSensor.sensorId)+"_"+str(p.endSensor.sensorId)] = [{"x": mp.x, "y": mp.y} for mp in Midpoint.objects.filter(path = p).order_by("order")]
		response["paths"][str(p.endSensor.sensorId)+"_"+str(p.startSensor.sensorId)] = [{"x": mp.x, "y": mp.y} for mp in Midpoint.objects.filter(path = p).order_by("-order")]
	return render(request, "hubnet/record_movements.html", {"tableData": response, "backgroundImage": bg})
		
'''
	LIVE VISUALIZATION
	Here are defined the views to get the needed information by the java application for live visualization
'''
def event_config(request, eventId):	
	data = { 	"background" : (settings.MEDIA_ROOT+"/"+str(Event.objects.get(id = eventId).background)).replace("\\","/"),
				"sensors": [{"sensorId": s.sensorId, "secondAnt": s.secondAnt, "xPos": s.x, "yPos": s.y, "name": s.name} for s in Sensor.objects.filter(event__id = eventId)],
				"groups" : [{"name": g.name, "color": g.color} for g in Group.objects.filter(event__id = eventId)],
				"participants" : [{"tagId": p.tagId, "color": p.group.color} for p in Participant.objects.filter(event__id = eventId)]}

	return HttpResponse(json.dumps(data));
	
'''
	RECORDINGS
	Here is defined the view to save the recordings got from the java server
'''
@csrf_exempt
def input_records(request):
	if request.method == 'POST':
		req = json.loads(request.body.decode("utf-8"))						# Get json data sent with the request
		
		event = Event.objects.get(id = req["eventID"])
		sensor = Sensor.objects.filter(event = event).get(sensorId = req["sensorID"])
		parts = Participant.objects.filter(event = event)
		
		# Save records in bulk
		records = []
		for tagId in req["tagIds"]:
			tempPart = parts.filter(tagId = tagId).first()
			if tempPart != None:
				records.append(Record(event=event, sensor=sensor, timestamp=req["timestamp"], participant = tempPart))
		Record.objects.bulk_create(records)
		
	return HttpResponse("ok")
	
'''
	PARTICIPANT REGISTRATION
	Here are defined the views to get or post the information needed by the java application 
	for participant registration and to save the registrations
'''
# Get all events for participant registration
def get_all_events(request):
	return HttpResponse(json.dumps({"events":[{"pk":e.pk, "name": e.name, "sensors": [s.sensorId for s in Sensor.objects.filter(event = e)]} for e in Event.objects.order_by('-id')]}));


# Get all events for participant registration
def get_groups(request, eventId):
	return HttpResponse(json.dumps({"groups":[{"pk":g.pk, "name": g.name} for g in Group.objects.filter(event__id = eventId)]}));

# Register participants for an event and a group
@csrf_exempt
def register_participants(request):
	if request.method == "POST":
		req = json.loads(request.body.decode("utf-8"))
		
		# Get event and group to save participants to
		event = Event.objects.get(id = req["eventId"])
		group = Group.objects.get(id = req["groupId"])
		
		for p in req["participants"]:
			Participant.objects.filter(tagId = p).filter(event__id = req["eventId"]).delete()

		# Save participants as a bulk
		parts = [Participant(event = event, group = group, tagId = p) for p in req["participants"]]
		Participant.objects.bulk_create(parts)
	return HttpResponse("ok")
	
'''
	ADMIN
	Here are defined the views used by the customized administration page
'''	
# Save a newly created event
@csrf_protect
def save_event(request):
	result = ""
	if request.method == "POST":
		# Get data from form to create a new event and validate the different fields
		req = request.POST
		
		# Validate the inputs
		name = req["name"]
		if not validate_charField(name, 20):
			result += "- Event name should be less than 20 characters\n"
		try:
			startDate = datetime.datetime.strptime(req["startDate"], "%Y-%m-%d %H:%M:%S")
		except:
			result += "- Incorrect date format for startDate, should be YYYY-MM-DD HH:MM:SS\n"
		try:
			stopDate = datetime.datetime.strptime(req["stopDate"], "%Y-%m-%d %H:%M:%S")
		except:
			result += "- Incorrect date format for stopDate, should be YYYY-MM-DD HH:MM:SS\n"

		groups = json.loads(req["groups"])

		if len(groups)==0:
			result += "- Select at least one group\n"
		
		obstacles = json.loads(req["obstacles"])
		sensors = json.loads(req["sensors"])
		paths = json.loads(req["paths"])
		floors = json.loads(req["floors"])
		
		# If validation is successful, then save everything, else send back the error to add_event.html
		if len(result) == 0:
			# Create event, with image if defined, without else
			if "backgroundImage" in request.FILES:
				event = Event(name = name, description = req["description"], startDate = startDate, stopDate = stopDate, background = request.FILES["backgroundImage"], obstacles = obstacles)
			else:
				event = Event(name = name, description = req["description"], startDate = startDate, stopDate = stopDate, obstacles = obstacles)
			event.save()
			event.groups.add(*list(Group.objects.filter(id__in=groups)))
			
			#Save sensors for event in bulk
			sensorObjs = [Sensor(event = event, sensorId = sens["id"], secondAnt = second_antenna(sens), x = sens["x"], y = sens["y"], name = sens["name"]) for sens in sensors]
			Sensor.objects.bulk_create(sensorObjs)
			
			# Save midpoints and paths in bulk
			midpointObjs = []
			
			for p in paths:
				tempPath = Path(event = event, startSensor = Sensor.objects.filter(event = event).get(sensorId = p["start"]), endSensor = Sensor.objects.filter(event = event).get(sensorId = p["end"]))
				tempPath.save()
				for idx, mp in enumerate(p["points"]):
					midpointObjs.append(Midpoint(path = tempPath, order = idx, x = mp["x"], y = mp["y"]))
					
			Midpoint.objects.bulk_create(midpointObjs)
			
			# Save floors in bulk
			fls = [Floor(event=event, name=f["name"], x=f["x"], y=f["y"], width=f["width"], height=f["height"]) for f in floors]
			Floor.objects.bulk_create(fls)
				
			return HttpResponse("ok")
		else:
			return HttpResponse(result)	

# Helper to handle non-existing secondary antennas
def second_antenna(sensor):
	try:
		return sensor["secondary_antenna"]
	except:
		return None
			
# Update an existing event			
@csrf_protect
def update_event(request):
	result = ""
	if request.method == "POST":
		# Get data from form to create a new event and validate the different fields
		req = request.POST
		
		# Validate the inputs
		name = req["name"]
		if not validate_charField(name, 20):
			result += "- Event name should be less than 20 characters\n"
		
		try:
			startDate = datetime.datetime.strptime(req["startDate"], "%Y-%m-%d %H:%M:%S")
		except:
			result += "- Incorrect date format for startDate, should be YYYY-MM-DD HH:MM:SS\n"
		
		try:
			stopDate = datetime.datetime.strptime(req["stopDate"], "%Y-%m-%d %H:%M:%S")
		except:
			result += "- Incorrect date format for stopDate, should be YYYY-MM-DD HH:MM:SS\n"

		groups = json.loads(req["groups"])

		if len(groups)==0:
			result += "- Select at least one group\n"
		
		obstacles = json.loads(req["obstacles"])
		sensors = json.loads(req["sensors"])
		paths = json.loads(req["paths"])
		floors = json.loads(req["floors"])
		
		# If validation is successful, then update everything, else send back the error to change_event.html
		if len(result) == 0:
			print(req["eventId"])
			event = Event.objects.get(id=int(req["eventId"]))				# Get the event to change
			Path.objects.filter(startSensor__event = event).delete()		# Remove all paths from this event
			event.groups.clear()											# Remove event-groups association
			
			# Update event, also background image if in request, without it otherwise
			if "backgroundImage" in request.FILES:
				event.background = request.FILES["backgroundImage"]
			event.name = name
			event.description = req["description"]
			event.startDate = startDate
			event.stopDate = stopDate
			event.obstacles = obstacles
			event.save()
			
			event.groups.add(*list(Group.objects.filter(id__in=groups)))	# Update groups related to the event
																				
			# Update sensors for event
			edited = []
			for sens in sensors:
				try:
					s = Sensor.objects.get(event=event, sensorId = sens["id"])
					try:
						s.secondAnt = sens["secondary_antenna"]
					except:
						s.secondAnt = None;
					s.x = sens["x"]
					s.y = sens["y"]
					s.name = sens["name"]
					s.save()
					edited.append(s.id)
				except Sensor.DoesNotExist:
					s = Sensor(event=event, sensorId=sens["id"], x=sens["x"], y=sens["y"], name=sens["name"])
					s.save();
					edited.append(s.id)
			
			Sensor.objects.filter(event = event).exclude(id__in=edited).delete()
			
			# Update midpoints and paths
			pathObjs = []
			midpointObjs = []
			
			for p in paths:
				tempPath = Path(event = event, startSensor = Sensor.objects.filter(event = event).get(sensorId = p["start"]), endSensor = Sensor.objects.filter(event = event).get(sensorId = p["end"]))
				tempPath.save()
				for idx, mp in enumerate(p["points"]):
					midpointObjs.append(Midpoint(path = tempPath, order = idx, x = mp["x"], y = mp["y"]))
					
			Midpoint.objects.bulk_create(midpointObjs)
			
			# Update floors
			Floor.objects.filter(event=event).delete()
			# Save floors in bulk
			fls = [Floor(event=event, name=f["name"], x=f["x"], y=f["y"], width=f["width"], height=f["height"]) for f in floors]
			Floor.objects.bulk_create(fls)
			
			return HttpResponse("ok")
		else:
			return HttpResponse(result)	
	
'''
	HELPERS
	Helpers for the custom admin interface
'''
# Get tables formatted as one wishes
def get_table(type, eventId, minTime = 20, sensors = None):	
	# Select events based on the structure of the url
	if eventId == None:
		events = list(Event.objects.all());
	else:
		events = list(Event.objects.filter(id = eventId))
		
	response = {"type": type, "filters" : {}, "data" : {}}						# Response structure common to all table types
	
	if type == "raw_records":
		# The possible fields that the user can chose to display/hide
		response["filters"] = {"Event": [{"name":"id", "type":"num"}, {"name":"name", "type":"str"}],
								"Participant": [{"name":"tagId", "type":"str"}, {"name":"color", "type":"str"}, {"name":"group", "type":"str"}],
								"Sensor": [{"name":"sensorId", "type":"num"}, {"name":"name", "type":"str"}, {"name":"x", "type":"num"}, {"name":"y", "type":"num"}], 
								"Detection": [{"name":"timestamp", "type":"dateTime"}]}
		
		# Significantly reduce database requests with this line, extreme execution time reduction! :)
		if sensors == None:
			records = Record.objects.prefetch_related("participant", "participant__group", "sensor", "event").all()
		else:
			records = Record.objects.prefetch_related("participant", "participant__group", "sensor", "event").filter(sensor__in = sensors)
		# Data as json
		response["data"] = [{"Event_id": r.event_id, "Event_name": r.event.name, "Detection_timestamp": str(r.timestamp.replace(microsecond=0)),
			"Participant_tagId": r.participant.tagId, "Participant_color": r.participant.group.color, "Participant_group": r.participant.group.name,
			"Sensor_sensorId": r.sensor.sensorId, "Sensor_name": r.sensor.name, "Sensor_x": round(r.sensor.x,2), "Sensor_y": round(r.sensor.y,2)}
			for r in records.filter(event__in = events)]
	elif type == "occupations":
		# The possible fields that the user can chose to display/hide
		response["filters"] = {"Event": [{"name":"id", "type":"num"}, {"name":"name", "type":"str"}],
								"Participant": [{"name":"tagId", "type":"str"}, {"name":"color", "type":"str"}, {"name":"group", "type":"str"}],
								"Sensor": [{"name":"sensorId", "type":"num"}, {"name":"name", "type":"str"}, {"name":"x", "type":"num"}, {"name":"y", "type":"num"}], 
								"Detection": [{"name":"start", "type":"dateTime"}, {"name":"end", "type":"dateTime"}, {"name":"duration", "type":"num"}]}
		
		# Significantly reduce database requests with this line, extreme execution time reduction! :)
		if sensors == None:
			records = Record.objects.prefetch_related("participant", "participant__group", "sensor", "event").all().order_by("participant", "timestamp")
		else:
			records = Record.objects.prefetch_related("participant", "participant__group", "sensor", "event").filter(sensor__in = sensors).order_by("participant", "timestamp")
		# Data as json
		start = records[0];
		stop = records[0];
		response["data"] = []
		# Take consecutive records of the same participant and event and see when the person entered or exited a room (minimum duration of room visit: 20 seconds)
		for r in records:
			if r.event_id != start.event_id or r.participant_id != start.participant_id :
				start = r
				stop = r
			elif r.sensor_id != start.sensor_id:
				temp = {"Event_id": r.event_id, "Event_name": r.event.name,
					"Participant_tagId": r.participant.tagId, "Participant_color": r.participant.group.color, "Participant_group": r.participant.group.name,
					"Sensor_sensorId": r.sensor.sensorId, "Sensor_name": r.sensor.name, "Sensor_x": round(r.sensor.x,2), "Sensor_y": round(r.sensor.y,2),
					"Detection_start": str(start.timestamp.replace(microsecond=0))}
				if (stop.timestamp - start.timestamp).total_seconds() >= minTime:	# If the difference in time is big enough, then the person has entered and then exited the room
					# update response with stop
					temp["Detection_end"] = str(stop.timestamp.replace(microsecond=0))
					temp["Detection_duration"] = int((stop.timestamp - start.timestamp).total_seconds())
					response["data"].append(temp)
				elif (r.timestamp - start.timestamp).total_seconds()-5 >= minTime:
					# update response with r
					temp["Detection_end"] = str(r.timestamp.replace(microsecond=0)- datetime.timedelta(seconds=5))
					temp["Detection_duration"] = int((r.timestamp - start.timestamp).total_seconds()-5)
					response["data"].append(temp)
				start = r
				stop = r
			else:
				stop = r
	
	elif type == "movements":
		# The possible fields that the user can chose to display/hide
		response["filters"] = {"Event": [{"name":"id", "type":"num"}, {"name":"name", "type":"str"}],
								"Participant": [{"name":"tagId", "type":"str"}, {"name":"color", "type":"str"}, {"name":"group", "type":"str"}],
								"startSensor": [{"name":"sensorId", "type":"num"}, {"name":"name", "type":"str"}, {"name":"x", "type":"num"}, {"name":"y", "type":"num"}], 
								"endSensor": [{"name":"sensorId", "type":"num"}, {"name":"name", "type":"str"}, {"name":"x", "type":"num"}, {"name":"y", "type":"num"}], 
								"Movement": [{"name":"start", "type":"dateTime"}, {"name":"end", "type":"dateTime"}, {"name":"duration", "type":"num"}]}
		
		# Significantly reduce database requests with this line, extreme execution time reduction! :)
		if sensors == None:
			records = Record.objects.prefetch_related("participant", "participant__group", "sensor", "event").all().order_by("participant", "timestamp")
		else:
			records = Record.objects.prefetch_related("participant", "participant__group", "sensor", "event").filter(sensor__in = sensors).order_by("participant", "timestamp")
		# Data as json
		start = records[0];
		response["data"] = []
		# Take consecutive records of the same participant and event and see when the parson entered or exited a room (minimum duration of room visit: 20 seconds)
		for r in records.filter(event__in = events):
			if r.event_id == start.event_id and r.participant_id == start.participant_id and r.sensor_id != start.sensor_id:
				temp = {"Event_id": r.event_id, "Event_name": r.event.name,
					"Participant_tagId": r.participant.tagId, "Participant_color": r.participant.group.color, "Participant_group": r.participant.group.name,
					"startSensor_sensorId": start.sensor.sensorId, "startSensor_name": start.sensor.name, "startSensor_x": round(start.sensor.x,2), "startSensor_y": round(start.sensor.y,2),
					"endSensor_sensorId": r.sensor.sensorId, "endSensor_name": r.sensor.name, "endSensor_x": round(r.sensor.x,2), "endSensor_y": round(r.sensor.y,2),
					"Movement_start": str(start.timestamp.replace(microsecond=0))}
				if (r.timestamp - start.timestamp).total_seconds() >= minTime:	# If the difference in time is big enough, then the person has left a room and then entered another one
					# update response with stop
					temp["Movement_end"] = str(r.timestamp.replace(microsecond=0))
					temp["Movement_duration"] = int((r.timestamp - start.timestamp).total_seconds())
					response["data"].append(temp)
			start = r
	else:
		return "Invalid table type"
	return response;


# Validate characters field
def validate_charField(charField, maxlength):
	if len(charField)>maxlength:
		return False
	else:
		return True
