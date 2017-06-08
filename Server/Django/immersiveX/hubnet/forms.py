#form.py
from django.forms import ModelForm
from django.forms.widgets import TextInput
from .models import Group


class GroupForm(ModelForm):
    class Meta:
        model = Group
        fields = "__all__"
        widgets = {
            "color": TextInput(attrs={"type": "color"}),
        }