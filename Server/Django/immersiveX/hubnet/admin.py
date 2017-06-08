from django.contrib import admin
from .models import Group, Participant, Sensor, Midpoint, Path, Event, Record, Floor
from .forms import GroupForm

import json
from django.core import serializers

'''
	admin.py IE 2.0
	Author: Moreno Colombo
	Creation date: 29.12.2016
	Last modified: 11.01.2017
'''

class RecordAdmin(admin.ModelAdmin):
	def delete_all_records(modeladmin, request, queryset):		
		Record.objects.all().delete()	
	
	actions = [delete_all_records]

class GroupAdmin(admin.ModelAdmin):
	form = GroupForm
	fields = ("name", "color")


# Use custom template to add and change Event
class EventAdmin(admin.ModelAdmin):
	fields = ("name", ("startDate", "stopDate"), "groups", "description", "background")
	add_form_template = "admin/add_event.html"
	
	def change_view(self, request, extra_context):
		eventId = int(extra_context)
		extra_context = {}
		event = Event.objects.get(id = eventId)
		# Add some content to populate the view
		extra_context["sensors"] = serialize_sensor(Sensor.objects.filter(event__id = eventId))
		extra_context["obstacles"] = event.obstacles
		extra_context["backgroundImage"] = event.background
		extra_context["eventId"] = eventId
		extra_context["paths"] = serialize_path(Path.objects.filter(event__id = eventId))
		extra_context["floors"] = serialize_floors(Floor.objects.filter(event__id = eventId))
		return super(EventAdmin, self).change_view(request, str(eventId), extra_context=extra_context)

	change_form_template = "admin/change_event.html"	
	
	def duplicate_selected_events(modeladmin, request, queryset):
		for e in queryset:
			sensors = Sensor.objects.filter(event = e)
			paths = Path.objects.filter(event = e)
			midpoints = Midpoint.objects.filter(path__in = paths)
			participants = Participant.objects.filter(event = e)
			records = Record.objects.filter(event = e)
			floors = Floor.objects.filter(event = e)
			e.pk = None
			e.name = e.name+"_copy"
			e.save()
			for s in sensors:
				s.pk = None
				s.event=e
				s.save()
			for p in paths:
				midpoints = Midpoint.objects.filter(path = p)
				p.pk = None
				p.event=e
				p.save()
				for m in midpoints:
					m.pk = None
					m.path = p
					m.save()
			for p in participants:
				p.pk = None
				p.event = e
				p.save()
			for r in records:
				r.pk = None
				r.event = e
				r.save()
			for f in floors:
				f.pk = None
				f.event = e
				f.save()
	
	actions = [duplicate_selected_events]
	
# Show models in admin mode
admin.site.register(Group, GroupAdmin)
admin.site.register(Participant)
admin.site.register(Sensor)
#admin.site.register(Midpoint)
#admin.site.register(Path)
#admin.site.register(Floor)
admin.site.register(Event, EventAdmin)
admin.site.register(Record, RecordAdmin)


###############################################################################################################################################
# Helper functions

def serialize_sensor(sensors):
	result = [{"id": s.sensorId, "secondary_antenna": s.secondAnt, "x": s.x, "y": s.y, "name": s.name} for s in sensors]
	return json.dumps(result, ensure_ascii=False)
	
def serialize_path(paths):
	result = []
	for p in paths:
		temp = {}
		temp["start"] = p.startSensor.sensorId
		temp["end"] = p.endSensor.sensorId
		temp["points"] = [{"x": po.x, "y": po.y} for po in Midpoint.objects.filter(path = p.id).order_by("order")]
		result.append(temp)
	return json.dumps(result, ensure_ascii=False)

def serialize_floors(floors):
	result=[{"name": f.name, "xMin": f.x, "yMin": f.y, "xMax": (f.x+f.width), "yMax": (f.y+f.height)} for f in floors]
	return json.dumps(result, ensure_ascii=False)
