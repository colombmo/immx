from django.db import models
from django.contrib.postgres.fields import ArrayField

'''
	models.py IE 2.0
	Author: Moreno Colombo
	Creation date: 28.12.2016
	Last modified: 10.01.2017
'''

# Group for categorizing people
class Group(models.Model):
	name = models.CharField(max_length=30)
	color = models.CharField(max_length=7)
	
	def __str__(self):
		return self.name	
		
# Define an event as a combination of different components
class Event(models.Model):
	name = models.CharField(max_length=20)
	description = models.TextField(max_length=500,blank=True)
	startDate = models.DateTimeField()
	stopDate = models.DateTimeField()
	groups = models.ManyToManyField(Group)
	background = models.FileField(blank=True, null=True)
	obstacles = ArrayField(ArrayField(models.FloatField()), null=True)
	
	def __str__(self):
		return self.name

# Define floors to divide event
class Floor(models.Model):
	event = models.ForeignKey(Event, null=True)
	name = models.CharField(max_length=20)
	x = models.FloatField()
	y = models.FloatField()
	width = models.FloatField()
	height = models.FloatField()
	
	def __str__(self):
		return self.event.name+"_"+self.name
	
# Infos about sensors placed at an event
class Sensor(models.Model):
	event = models.ForeignKey(Event, null=True)
	sensorId = models.IntegerField()
	secondAnt = models.IntegerField(null=True)
	x = models.FloatField()
	y = models.FloatField()
	name = models.CharField(max_length=20)

	def __str__(self):
		return str(self.sensorId)+" "+self.name				
		
# Path between two sensors that participants follow
class Path(models.Model):
	event = models.ForeignKey(Event, null=True)
	startSensor = models.ForeignKey(Sensor, related_name="start_sensor")
	endSensor = models.ForeignKey(Sensor, related_name="end_sensor")
	
	def __str__(self):
		return str(self.startSensor)+" -> "+ str(self.endSensor)		


# Point between two sensor to define paths
class Midpoint(models.Model):
	order = models.IntegerField(null=True)
	x = models.FloatField()
	y = models.FloatField()
	path = models.ForeignKey(Path, null=True)
	
	def __str__(self):
		return "("+str(self.x)+", "+str(self.y)+")";		
		
# Infos about people participating to the event
class Participant(models.Model):
	event = models.ForeignKey(Event, null=True)
	tagId = models.CharField(max_length=30)
	group = models.ForeignKey(Group)
	
	def __str__(self):
		return self.tagId
		

# Keep track of detection of people at a sensor
class Record(models.Model):
	event = models.ForeignKey(Event)
	sensor = models.ForeignKey(Sensor)
	timestamp = models.DateTimeField()
	participant = models.ForeignKey(Participant)
	
	def __str__(self):
		return str(self.participant)